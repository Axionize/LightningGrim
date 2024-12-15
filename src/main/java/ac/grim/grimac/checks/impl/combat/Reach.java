// This file was designed and is an original check for GrimAC
// Copyright (C) 2021 DefineOutside
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BlockHitData;
import ac.grim.grimac.utils.data.EntityHitData;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.nmsutil.BlockRayTrace;
import ac.grim.grimac.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// You may not copy the check unless you are licensed under GPL
@CheckData(name = "Reach", configName = "Reach", setback = 10)
public class Reach extends Check implements PacketCheck {
    // Only one flag per reach attack, per entity, per tick.
    // We store position because lastX isn't reliable on teleports.
    private final Int2ObjectMap<Vector3d> playerAttackQueue = new Int2ObjectOpenHashMap<>();
    // Used to prevent falses in the wall hit check
    private final Set<Vector3i> blocksChangedThisTick = new HashSet<>();

    private static final List<EntityType> blacklisted = Arrays.asList(
            EntityTypes.BOAT,
            EntityTypes.CHEST_BOAT,
            EntityTypes.SHULKER);

    private static final double ENTITY_HITBOX_REACH_EPSILON = 1E-12;

    private boolean ignoreNonPlayerTargets;
    private boolean skipBlockCheck;
    private boolean skipEntityCheck;
    private boolean cancelImpossibleHits;
    public double reachThreshold;
    private double cancelBuffer; // For the next 4 hits after using reach, we aggressively cancel reach

    public Reach(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);

            // Don't let the player teleport to bypass reach
            if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
                event.setCancelled(true);
                player.onPacketCancel();
                return;
            }

            PacketEntity entity = player.compensatedEntities.entityMap.get(action.getEntityId());
            // Stop people from freezing transactions before an entity spawns to bypass reach
            // TODO: implement dragon parts?
            if (entity == null || entity instanceof PacketEntityEnderDragonPart) {
                // Only cancel if and only if we are tracking this entity
                // This is because we don't track paintings.
                if (shouldModifyPackets() && player.compensatedEntities.serverPositionsMap.containsKey(action.getEntityId())) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                return;
            }

            if (ignoreNonPlayerTargets && !entity.getType().equals(EntityTypes.PLAYER)) {
                return;
            }

            // Dead entities cause false flags (https://github.com/GrimAnticheat/Grim/issues/546)
            if (entity.isDead) return;

            // TODO: Remove when in front of via
            if (entity.getType() == EntityTypes.ARMOR_STAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) return;

            if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
            if (player.compensatedEntities.getSelf().inVehicle()) return;
            if (entity.riding != null) return;

            boolean tooManyAttacks = playerAttackQueue.size() > 10;
            if (!tooManyAttacks) {
                playerAttackQueue.put(action.getEntityId(), new Vector3d(player.x, player.y, player.z)); // Queue for next tick for very precise check
            }

            boolean knownInvalid = isKnownInvalid(entity);

            if ((shouldModifyPackets() && cancelImpossibleHits && knownInvalid) || tooManyAttacks) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        // If the player set their look, or we know they have a new tick
        final boolean isFlying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        if (isUpdate(event.getPacketType())) {
            tickBetterReachCheckWithAngle(isFlying);
        }
    }

    // This method finds the most optimal point at which the user should be aiming at
    // and then measures the distance between the player's eyes and this target point
    //
    // It will not cancel every invalid attack but should cancel 3.05+ or so in real-time
    // Let the post look check measure the distance, as it will always return equal or higher
    // than this method.  If this method flags, the other method WILL flag.
    //
    // Meaning that the other check should be the only one that flags.
    private boolean isKnownInvalid(PacketEntity reachEntity) {
        // If the entity doesn't exist, or if it is exempt, or if it is dead
        if ((blacklisted.contains(reachEntity.getType()) || !reachEntity.isLivingEntity()) && reachEntity.getType() != EntityTypes.END_CRYSTAL)
            return false; // exempt

        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return false;
        if (player.compensatedEntities.getSelf().inVehicle()) return false;

        // Filter out what we assume to be cheats
        if (cancelBuffer != 0) {
            return checkReach(reachEntity, new Vector3d(player.x, player.y, player.z), true) != null; // If they flagged
        } else {
            SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();
            if (reachEntity.getType() == EntityTypes.END_CRYSTAL) {
                targetBox = new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
            }
            return ReachUtils.getMinReachToBox(player, targetBox) > player.compensatedEntities.getSelf().getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        }
    }

    private void tickBetterReachCheckWithAngle(boolean isFlying) {
        for (Int2ObjectMap.Entry<Vector3d> attack : playerAttackQueue.int2ObjectEntrySet()) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attack.getIntKey());
            if (reachEntity != null) {
                Pair<Class<? extends PacketCheck>, String> result = checkReach(reachEntity, attack.getValue(), false);
                if (result != null) {
                    PacketCheck check = player.checkManager.getPacketCheck(result.getFirst());
                    if (reachEntity.getType() == EntityTypes.PLAYER) {
                        ((Check)check).flagAndAlert(result.getSecond());
                    } else {
                        ((Check) check).flagAndAlert(result.getSecond() + "type=" + reachEntity.getType().getName().getKey());
                    }
                }
            }
        }
        playerAttackQueue.clear();
        // We can't use transactions for this because of this problem:
        // transaction -> block changed applied -> 2nd transaction -> list cleared -> attack packet -> flying -> reach block hit checked, falses
        if (isFlying) blocksChangedThisTick.clear();
    }

    private Pair<Class<? extends PacketCheck>, String> checkReach(PacketEntity reachEntity, Vector3d from, boolean isPrediction) {
        SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();

        if (reachEntity.getType() == EntityTypes.END_CRYSTAL) { // Hardcode end crystal box
            targetBox = new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
        }

        // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
        // Yes, this is vanilla and not uncertainty.  All reach checks have this or they are wrong.
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
            targetBox.expand(0.1f);
        }

        targetBox.expand(reachThreshold);

        // This is better than adding to the reach, as 0.03 can cause a player to miss their target
        // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
        //
        // Just give the uncertainty on 1.9+ clients as we have no way of knowing whether they had 0.03 movement
        if (!player.packetStateData.didLastLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9))
            targetBox.expand(player.getMovementThreshold());

        double minDistance = Double.MAX_VALUE;

        // https://bugs.mojang.com/browse/MC-67665
        List<Vector> possibleLookDirs = new ArrayList<>(Collections.singletonList(ReachUtils.getLook(player, player.xRot, player.yRot)));

        // If we are a tick behind, we don't know their next look so don't bother doing this
        if (!isPrediction) {
            possibleLookDirs.add(ReachUtils.getLook(player, player.lastXRot, player.yRot));

            // 1.9+ players could be a tick behind because we don't get skipped ticks
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                possibleLookDirs.add(ReachUtils.getLook(player, player.lastXRot, player.lastYRot));
            }

            // 1.7 players do not have any of these issues! They are always on the latest look vector
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
                possibleLookDirs = Collections.singletonList(ReachUtils.getLook(player, player.xRot, player.yRot));
            }
        }

        // We raytrace for > the player's reach distance so in the case a player is hacking
        // We can return in the flag the distance of the reach hit instead of a generic "player failed reach check"
        // +3 would be 3 + 3 = 6, which is the pre-1.20.5 behaviour, preventing "Missed Hitbox"
        final double distance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + 3;
        double[] possibleEyeHeights = player.getPossibleEyeHeights();
//        possibleEyeHeights = new double[]{possibleEyeHeights[0]};
//        double realMinDistance = 0;
        for (Vector lookVec : possibleLookDirs) {
            for (double eye : possibleEyeHeights) {
                Vector eyePos = new Vector(from.getX(), from.getY() + eye, from.getZ());
                Vector endReachPos = eyePos.clone().add(new Vector(lookVec.getX() * distance, lookVec.getY() * distance, lookVec.getZ() * distance));

                Vector intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).getFirst();

                if (ReachUtils.isVecInside(targetBox, eyePos)) {
                    minDistance = 0;
                    break;
                }

                if (intercept != null) {
                    minDistance = Math.min(eyePos.distance(intercept), minDistance);
//                    if (eye == possibleEyeHeights[0]) {
//                        realMinDistance = minDistance;
//                    }
                }
            }
        }

        HitData foundHitData = null;
        // If the entity is within range of the player (we'll flag anyway if not, so no point checking blocks in this case)
        // Ignore when could be hitting through a moving shulker, piston blocks. They are just too glitchy/uncertain to check.
        if (!skipEntityCheck || !skipBlockCheck) {
            if (minDistance <= distance - 3 && !player.compensatedWorld.isNearHardEntity(player.boundingBox.copy().expand(4))) {
                final @Nullable Pair<Double, HitData> hitResult = didRayTraceHit(reachEntity, possibleLookDirs, from, minDistance);

                // if the hitResult is closer to the player than the target entity box, they shouldn't have hit the target entity
                // We are checking if the diff > epsilon because the hit distance returned is slightly different due to floating point shennanigans
                // - This filters out when the ray trace hits the target entity without having to do an expensive .equals()
                // - You may have to adjust the epsilon if you increase the reach threshold, especially by a lot
                // ...but there is literally no reason you would ever want to increase it, only decrease, so that doesn't matter.
                if (hitResult != null && (minDistance * minDistance) - hitResult.getFirst() > ENTITY_HITBOX_REACH_EPSILON) { // returned double is distanceSq
                    minDistance = Double.MIN_VALUE;
                    foundHitData = hitResult.getSecond();
                }
            }
        }

        // if the entity is not exempt and the entity is alive
        if ((!blacklisted.contains(reachEntity.getType()) && reachEntity.isLivingEntity()) || reachEntity.getType() == EntityTypes.END_CRYSTAL) {
            if (minDistance == Double.MIN_VALUE && foundHitData != null) {
                cancelBuffer = 1;
                if (foundHitData instanceof BlockHitData) {
                    return new Pair<>(HitboxBlock.class, "Hit block=" + ((BlockHitData) foundHitData).getState().getType().getName() + " ");
                } else { // entity hit data
                    return new Pair<>(HitboxEntity.class, "Hit entity=" + ((EntityHitData) foundHitData).getEntity().getType().getName() + " ");
                }
            } else if (minDistance == Double.MAX_VALUE) {
                cancelBuffer = 1;
                return new Pair<>(HitboxMiss.class, "");
            } else if (minDistance > player.compensatedEntities.getSelf().getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)) {
                cancelBuffer = 1;
                return new Pair<>(Reach.class, String.format("%.5f", minDistance) + " blocks");
            } else {
                cancelBuffer = Math.max(0, cancelBuffer - 0.25);
            }
        }

        return null;
    }

    public void handleBlockChange(Vector3i vector3i, WrappedBlockState state) {
        if (blocksChangedThisTick.size() >= 40) return; // Don't let players freeze movement packets to grow this
        // Only do this for nearby blocks
        if (new Vector(vector3i.x, vector3i.y, vector3i.z).distanceSquared(new Vector(player.x, player.y, player.z)) > 6) return;
        // Only do this if the state really had any world impact
        if (state.equals(player.compensatedWorld.getWrappedBlockStateAt(vector3i))) return;
        blocksChangedThisTick.add(vector3i);
    }

    // Checks if it was possible to hit a target entity
    @Nullable
    private Pair<Double, HitData> didRayTraceHit(PacketEntity targetEntity, List<Vector> possibleLookDirs, Vector3d from, double minDistance) {
        HitData bestHitData = null;
        double bestDistanceSq = Double.MAX_VALUE;
        double bestBlockingEntityDistanceSq = Double.MAX_VALUE;
        HitData bestBlockingEntityHit = null;

        // Check every possible look direction and every possible eye height
        // IF *NONE* of them allow the player to hit the entity, this is an invalid hit
        for (Vector lookVec : possibleLookDirs) {
            for (double eye : player.getPossibleEyeHeights()) {
                Vector eyes = new Vector(from.getX(), from.getY() + eye, from.getZ());
                // this function is completely 0.03 aware
                final HitData hitResult = BlockRayTrace.getNearestHitResult(player, targetEntity, eyes, lookVec, minDistance, skipBlockCheck, skipEntityCheck);

                if (hitResult == null)
                    if (skipEntityCheck)
                        return null;
                    else
                        continue;

                double distanceSquared = eyes.distanceSquared(hitResult.getBlockHitLocation());

                // Check if the hit result is the target entity
                if (hitResult instanceof EntityHitData && ((EntityHitData) hitResult).getEntity().equals(targetEntity)) {
                    return new Pair<>(distanceSquared, hitResult); // Legitimate hit
                }

                // Check for potential blocking entities
                if (hitResult instanceof EntityHitData) {
                    // Consider entities that are blocking the path to the target
                    if (distanceSquared < bestBlockingEntityDistanceSq) {
                        bestBlockingEntityDistanceSq = distanceSquared;
                        bestBlockingEntityHit = hitResult;
                    }
                } else if (hitResult instanceof BlockHitData) {
                    // don't false on recently rapidly changed blocks
                    if (distanceSquared < (minDistance * minDistance) && blocksChangedThisTick.contains(((BlockHitData) hitResult).getPosition())) {
                        return null;
                    }
                    // Check if block is closer than any blocking entity found
                    if (bestBlockingEntityHit == null && distanceSquared < bestDistanceSq) {
                        bestDistanceSq = distanceSquared;
                        bestHitData = hitResult;
                    }
                }
            }
        }

        // Prefer returning the entity that blocks the path to the target entity
        if (bestBlockingEntityHit != null) {
            return new Pair<>(bestBlockingEntityDistanceSq, bestBlockingEntityHit);
        }

        // If no blocking entity, return the closest block
        return bestHitData == null ? null : new Pair<>(bestDistanceSq, bestHitData);
    }

    @Override
    public void onReload(ConfigManager config) {
        this.ignoreNonPlayerTargets = config.getBooleanElse("Reach.ignore-non-player-targets", false);
        this.skipBlockCheck = config.getBooleanElse("Reach.skip-block-check", false);
        this.skipEntityCheck = config.getBooleanElse("Reach.skip-entity-check", true);
        this.cancelImpossibleHits = config.getBooleanElse("Reach.block-impossible-hits", true);
        this.reachThreshold = config.getDoubleElse("Reach.threshold", 0.0005);
    }
}
