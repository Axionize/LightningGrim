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
import ac.grim.grimac.checks.debug.HitboxDebugHandler;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;

// You may not copy the check unless you are licensed under GPL
@CheckData(name = "Reach", setback = 10)
public class Reach extends Check implements PacketCheck {

    // Only one flag per reach attack, per entity, per tick.
    // We store position because lastX isn't reliable on teleports.
    private final Int2ObjectMap<Vector3d> playerAttackQueue = new Int2ObjectOpenHashMap<>();
    // Used to prevent falses in the wall hit check
    private final Set<Vector3i> blocksChangedThisTick = new HashSet<>();

    public static final double extraSearchDistance = 3; // extra distance to raytrace beyond reach distance so we know far beyond the legit distance a cheater hit

    private static final List<EntityType> blacklisted = Arrays.asList(
            EntityTypes.BOAT,
            EntityTypes.CHEST_BOAT,
            EntityTypes.SHULKER);

    private boolean ignoreNonPlayerTargets;
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
            if (player.inVehicle()) return;
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
        if (player.inVehicle()) return false;

        // Filter out what we assume to be cheats
        if (cancelBuffer != 0) {
            return checkReach(reachEntity, new Vector3d(player.x, player.y, player.z), true) != NONE; // If they flagged
        } else {
            SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();
            if (reachEntity.getType() == EntityTypes.END_CRYSTAL) {
                targetBox = new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
            }
            return ReachUtils.getMinReachToBox(player, targetBox) > player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        }
    }

    private void tickBetterReachCheckWithAngle(boolean isFlying) {
        for (Int2ObjectMap.Entry<Vector3d> attack : playerAttackQueue.int2ObjectEntrySet()) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attack.getIntKey());
            if (reachEntity == null) continue;

            CheckResult result = checkReach(reachEntity, attack.getValue(), false);
            String added;
            switch (result.type()) {
                case REACH:
                    added = reachEntity.getType() == EntityTypes.PLAYER ? "" : ", type=" + reachEntity.getType().getName().getKey();
                    flagAndAlert(result.verbose() + added);
                    break;
                case HITBOX:
                    added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getPacketCheck(HitboxMiss.class).flagAndAlert(result.verbose() + added);
                    break;
                case BLOCK:
                    added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getPacketCheck(HitboxBlock.class).flagAndAlert(result.verbose() + added);
                    break;
                case ENTITY:
                    added = reachEntity.getType() == EntityTypes.PLAYER ? "" : "type=" + reachEntity.getType().getName().getKey();
                    player.checkManager.getPacketCheck(HitboxEntity.class).flagAndAlert(result.verbose() + added);
                    break;
            }
        }

        playerAttackQueue.clear();
        // We can't use transactions for this because of this problem:
        // transaction -> block changed applied -> 2nd transaction -> list cleared -> attack packet -> flying -> reach block hit checked, falses
        if (isFlying) blocksChangedThisTick.clear();
    }

    @NotNull
    private CheckResult checkReach(PacketEntity reachEntity, Vector3d from, boolean isPrediction) {
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
        // However, on 1.21.2+ we do know if they had 0.03 movement
        if (!player.packetStateData.didLastLastMovementIncludePosition || player.canSkipTicks())
            targetBox.expand(player.getMovementThreshold());

        double minDistance = Double.MAX_VALUE;

        // will store all lookVecsAndEyeHeight pairs that landed a hit on the target entity
        // We only need to check for blocking intersections for these
        List<Pair<Vector, Double>> lookVecsAndEyeHeights = new ArrayList<>();

        final double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        // We raytrace for > the player's reach distance so in the case a player is hacking
        // We can return in the flag the distance of the reach hit instead of a generic "player failed reach check"
        // +3 would be 3 + extraSearchDistance = 6, which is the pre-1.20.5 behaviour, preventing "Missed Hitbox"
        final double distance = maxReach + extraSearchDistance;
        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        final Vector[] possibleLookDirs = player.getPossibleLookVectors(isPrediction);
        final Vector eyePos = new Vector(from.getX(), 0, from.getZ());
        for (Vector lookVec : possibleLookDirs) {
            for (double eye : possibleEyeHeights) {
                eyePos.setY(from.getY() + eye);
                Vector endReachPos = eyePos.clone().add(new Vector(lookVec.getX() * distance, lookVec.getY() * distance, lookVec.getZ() * distance));

                Vector intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).first();

                if (ReachUtils.isVecInside(targetBox, eyePos)) {
                    minDistance = 0;
                    break;
                }

                if (intercept != null) {
                    minDistance = Math.min(eyePos.distance(intercept), minDistance);
                    lookVecsAndEyeHeights.add(new Pair<>(lookVec, eye));
                }
            }
        }

        if (hitboxDebuggingEnabled())
            sendHitboxDebugData(reachEntity, from, lookVecsAndEyeHeights, isPrediction);

        HitData foundHitData = null;
        // If the entity is within range of the player (we'll flag anyway if not, so no point checking blocks in this case)
        // Ignore when could be hitting through a moving shulker, piston blocks. They are just too glitchy/uncertain to check.
        if (minDistance <= distance - extraSearchDistance && !player.compensatedWorld.isNearHardEntity(player.boundingBox.copy().expand(4))) {
            final @Nullable Pair<Double, HitData> hitResult = didRayTraceHit(reachEntity, lookVecsAndEyeHeights, from, minDistance);
            HitData hitData = hitResult.second();
            // If the returned hit result was NOT the target entity we flag the check
            if (!(hitData instanceof EntityHitData &&
                    player.compensatedEntities.getPacketEntityID(((EntityHitData) hitData).getEntity()) == player.compensatedEntities.getPacketEntityID(reachEntity))) {
                minDistance = Double.MIN_VALUE;
                foundHitData = hitData;
            }
        }

        // if the entity is not exempt and the entity is alive
        if ((!blacklisted.contains(reachEntity.getType()) && reachEntity.isLivingEntity()) || reachEntity.getType() == EntityTypes.END_CRYSTAL) {
            if (minDistance == Double.MIN_VALUE && foundHitData != null) {
                cancelBuffer = 1;
                if (foundHitData instanceof BlockHitData) {
                    return new CheckResult(ResultType.BLOCK, "Hit block=" + ((BlockHitData) foundHitData).getState().getType().getName() + " ");
                } else { // entity hit data
                    return new CheckResult(ResultType.ENTITY, "Hit entity=" + ((EntityHitData) foundHitData).getEntity().getType().getName() + " ");
                }
            } else if (minDistance == Double.MAX_VALUE) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.HITBOX, "");
            } else if (minDistance > player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.REACH, String.format("%.5f", minDistance) + " blocks");
            } else {
                cancelBuffer = Math.max(0, cancelBuffer - 0.25);
            }
        }

        return NONE;
    }

    private static final CheckResult NONE = new CheckResult(ResultType.NONE, "");

    private static class CheckResult {
        private final ResultType type;
        private final String verbose;

        public CheckResult(ResultType type, String verbose) {
            this.type = type;
            this.verbose = verbose;
        }

        public ResultType type() {
            return type;
        }

        public String verbose() {
            return verbose;
        }

        public boolean isFlag() {
            return type != ResultType.NONE;
        }
    }

    private enum ResultType {
        REACH, HITBOX, BLOCK, ENTITY, NONE
    }

    public void handleBlockChange(Vector3i vector3i, WrappedBlockState state) {
        if (blocksChangedThisTick.size() >= 40) return; // Don't let players freeze movement packets to grow this
        // Only do this for nearby blocks
        if (new Vector(vector3i.x, vector3i.y, vector3i.z).distanceSquared(new Vector(player.x, player.y, player.z)) > 6) return;
        // Only do this if the state really had any world impact
        if (state.equals(player.compensatedWorld.getBlock(vector3i))) return;
        blocksChangedThisTick.add(vector3i);
    }

    // Checks if it was possible to hit a target entity
    // TODO refactor to return list of rays and why each of them didn't hit instead of closest obstruction
    // NOTE: It should be impossible for the returned Pair to be null
    // because all of the possibleLookVecsAndEyeHeights passed in should be ones that hit the target entity
    // in previous parts of this check when we didn't check for any obstructions like blocks/entities
    private @NotNull Pair<@NotNull Double, @NotNull HitData> didRayTraceHit(PacketEntity targetEntity,
                                                                            List<Pair<Vector, Double>> possibleLookVecsAndEyeHeights,
                                                                            Vector3d from, double minDistance) {
        HitData firstObstruction = null;
        double firstObstructionDistanceSq = 0;

        // Check every possible look direction and every possible eye height
        for (Pair<Vector, Double> vectorDoublePair : possibleLookVecsAndEyeHeights) {
            Vector lookVec = vectorDoublePair.first();
            double eye = vectorDoublePair.second();

            Vector eyes = new Vector(from.getX(), from.getY() + eye, from.getZ());
            // this function is completely 0.03 aware
            final HitData hitResult = BlockRayTrace.getNearestHitResult(player, targetEntity, eyes, lookVec, minDistance);

            // If we hit the target entity, it's a valid hit
            if (hitResult instanceof EntityHitData && ((EntityHitData) hitResult).getEntity().equals(targetEntity)) {
                double distanceSquared = eyes.distanceSquared(hitResult.getBlockHitLocation());
                return new Pair<>(distanceSquared, hitResult); // Legitimate hit
            } else if (hitResult != null && firstObstruction == null) {
                // Store the first obstruction only
                firstObstruction = hitResult;
                firstObstructionDistanceSq = eyes.distanceSquared(hitResult.getBlockHitLocation());
            }
        }

        // Return the first obstruction if no valid hit found
        // Since we sort eye heights by likeniness, we should in effect return the most likely (first) obstruction
        assert firstObstruction != null;
        return new Pair<>(firstObstructionDistanceSq, firstObstruction);
    }

    private boolean hitboxDebuggingEnabled() {
        return player.checkManager.getCheck(HitboxDebugHandler.class).isEnabled();
    }

    private void sendHitboxDebugData(PacketEntity reachEntity, Vector3d from, List<Pair<Vector, Double>> lookVecsAndEyeHeights, boolean isPrediction) {
        Map<Integer, CollisionBox> hitboxes = new HashMap<>();
        for (Int2ObjectMap.Entry<PacketEntity> entry : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
            PacketEntity entity = entry.getValue();
            if (!entity.canHit()) continue;

            CollisionBox box;

            if (entity.equals(reachEntity)) {
                // Target entity gets expanded hitbox
                box = entity.getPossibleCollisionBoxes();
                SimpleCollisionBox sBox = (SimpleCollisionBox) box;
                sBox.expand(player.checkManager.getPacketCheck(Reach.class).reachThreshold);

                // Add movement threshold uncertainty for 1.9+ or non-position updates
                if (!player.packetStateData.didLastLastMovementIncludePosition
                        || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                    sBox.expand(player.getMovementThreshold());
                }
            } else {
                // Non-target entities
                box = entity.getMinimumPossibleCollisionBoxes();
                if (box instanceof NoCollisionBox) {
                    hitboxes.put(entry.getIntKey(), NoCollisionBox.INSTANCE);
                    continue;
                } else if (box instanceof SimpleCollisionBox) {
                    SimpleCollisionBox sBox = (SimpleCollisionBox) box;
                    sBox.expand(-player.checkManager.getPacketCheck(Reach.class).reachThreshold);
                    // Shrink non-target entities by movement threshold when applicable
                    if (!player.packetStateData.didLastLastMovementIncludePosition
                            || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                        sBox.expand(-player.getMovementThreshold());
                    }
                }
            }

            // Add 1.8 and below extra hitbox size
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)
                    && box instanceof SimpleCollisionBox) {
                ((SimpleCollisionBox) box).expand(0.1f);
            }

            hitboxes.put(entry.getIntKey(), box);
        }

        player.checkManager.getCheck(HitboxDebugHandler.class).sendHitboxData(hitboxes,
                Collections.singleton(player.compensatedEntities.getPacketEntityID(reachEntity)),
                lookVecsAndEyeHeights,
                new Vector(from.getX(), from.getY(), from.getZ()),
                isPrediction, player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
    }

    @Override
    public void onReload(ConfigManager config) {
        this.ignoreNonPlayerTargets = config.getBooleanElse("Reach.ignore-non-player-targets", false);
        this.cancelImpossibleHits = config.getBooleanElse("Reach.block-impossible-hits", true);
        this.reachThreshold = config.getDoubleElse("Reach.threshold", 0.0005);
    }
}
