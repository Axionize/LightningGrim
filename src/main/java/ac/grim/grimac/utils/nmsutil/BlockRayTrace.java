package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.combat.Reach;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.change.BlockModification;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.RaycastData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BlockHitData;
import ac.grim.grimac.utils.data.EntityHitData;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.TypedPacketEntity;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class BlockRayTrace {

    // Copied from MCP...
    // Returns null if there isn't anything.
    //
    // I do have to admit that I'm starting to like bifunctions/new java 8 things more than I originally did.
    // although I still don't understand Mojang's obsession with streams in some of the hottest methods... that kills performance
    @Nullable
    public static Pair<int[], BlockFace> traverseBlocksLOSP(GrimPlayer player, double[] start, double[] end, BiFunction<WrappedBlockState, int[], Pair<int[], BlockFace>> predicate) {
        // I guess go back by the collision epsilon?
        double endX = GrimMath.lerp(-1.0E-7D, end[0], start[0]);
        double endY = GrimMath.lerp(-1.0E-7D, end[1], start[1]);
        double endZ = GrimMath.lerp(-1.0E-7D, end[2], start[2]);
        double startX = GrimMath.lerp(-1.0E-7D, start[0], end[0]);
        double startY = GrimMath.lerp(-1.0E-7D, start[1], end[1]);
        double startZ = GrimMath.lerp(-1.0E-7D, start[2], end[2]);

        int[] floorStart = new int[]{GrimMath.floor(startX), GrimMath.floor(startY), GrimMath.floor(startZ)};

        if (start[0] == end[0] && start[1] == end[1] && start[2] == end[2]) return null;

        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(floorStart[0], floorStart[1], floorStart[2]);
        Pair<int[], BlockFace> apply = predicate.apply(state, floorStart);

        if (apply != null) {
            return apply;
        }

        double xDiff = endX - startX;
        double yDiff = endY - startY;
        double zDiff = endZ - startZ;
        double xSign = Math.signum(xDiff);
        double ySign = Math.signum(yDiff);
        double zSign = Math.signum(zDiff);

        double posXInverse = xSign == 0 ? Double.MAX_VALUE : xSign / xDiff;
        double posYInverse = ySign == 0 ? Double.MAX_VALUE : ySign / yDiff;
        double posZInverse = zSign == 0 ? Double.MAX_VALUE : zSign / zDiff;

        double tMaxX = posXInverse * (xSign > 0 ? 1.0D - GrimMath.frac(startX) : GrimMath.frac(startX));
        double tMaxY = posYInverse * (ySign > 0 ? 1.0D - GrimMath.frac(startY) : GrimMath.frac(startY));
        double tMaxZ = posZInverse * (zSign > 0 ? 1.0D - GrimMath.frac(startZ) : GrimMath.frac(startZ));

        // tMax represents the maximum distance along each axis before crossing a block boundary
        // The loop continues as long as the ray hasn't reached its end point along at least one axis.
        // In each iteration, it moves to the next block boundary along the axis with the smallest tMax value,
        // updates the corresponding coordinate, and checks for a hit in the new block, Google "3D DDA" for more info
        while (tMaxX <= 1.0D || tMaxY <= 1.0D || tMaxZ <= 1.0D) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    floorStart[0] += xSign;
                    tMaxX += posXInverse;
                } else {
                    floorStart[2] += zSign;
                    tMaxZ += posZInverse;
                }
            } else if (tMaxY < tMaxZ) {
                floorStart[1] += ySign;
                tMaxY += posYInverse;
            } else {
                floorStart[2] += zSign;
                tMaxZ += posZInverse;
            }

            state = player.compensatedWorld.getWrappedBlockStateAt(floorStart[0], floorStart[1], floorStart[2]);
            apply = predicate.apply(state, floorStart);

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }

    public static HitData traverseBlocks(GrimPlayer player, double[] start, double[] end, BiFunction<WrappedBlockState, Vector3i, HitData> predicate) {
        // I guess go back by the collision epsilon?
        double endX = GrimMath.lerp(-1.0E-7D, end[0], start[0]);
        double endY = GrimMath.lerp(-1.0E-7D, end[1], start[1]);
        double endZ = GrimMath.lerp(-1.0E-7D, end[2], start[2]);
        double startX = GrimMath.lerp(-1.0E-7D, start[0], end[0]);
        double startY = GrimMath.lerp(-1.0E-7D, start[1], end[1]);
        double startZ = GrimMath.lerp(-1.0E-7D, start[2], end[2]);
        int floorStartX = GrimMath.floor(startX);
        int floorStartY = GrimMath.floor(startY);
        int floorStartZ = GrimMath.floor(startZ);

        if (start[0] == end[0] && start[1] == end[1] && start[2] == end[2]) return null;

        if (start.equals(end)) return null;

        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
        HitData apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

        if (apply != null) {
            return apply;
        }

        double xDiff = endX - startX;
        double yDiff = endY - startY;
        double zDiff = endZ - startZ;
        double xSign = Math.signum(xDiff);
        double ySign = Math.signum(yDiff);
        double zSign = Math.signum(zDiff);

        double posXInverse = xSign == 0 ? Double.MAX_VALUE : xSign / xDiff;
        double posYInverse = ySign == 0 ? Double.MAX_VALUE : ySign / yDiff;
        double posZInverse = zSign == 0 ? Double.MAX_VALUE : zSign / zDiff;

        double tMaxX = posXInverse * (xSign > 0 ? 1.0D - GrimMath.frac(startX) : GrimMath.frac(startX));
        double tMaxY = posYInverse * (ySign > 0 ? 1.0D - GrimMath.frac(startY) : GrimMath.frac(startY));
        double tMaxZ = posZInverse * (zSign > 0 ? 1.0D - GrimMath.frac(startZ) : GrimMath.frac(startZ));

        // tMax represents the maximum distance along each axis before crossing a block boundary
        // The loop continues as long as the ray hasn't reached its end point along at least one axis.
        // In each iteration, it moves to the next block boundary along the axis with the smallest tMax value,
        // updates the corresponding coordinate, and checks for a hit in the new block, Google "3D DDA" for more info
        while (tMaxX <= 1.0D || tMaxY <= 1.0D || tMaxZ <= 1.0D) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    floorStartX += xSign;
                    tMaxX += posXInverse;
                } else {
                    floorStartZ += zSign;
                    tMaxZ += posZInverse;
                }
            } else if (tMaxY < tMaxZ) {
                floorStartY += ySign;
                tMaxY += posYInverse;
            } else {
                floorStartZ += zSign;
                tMaxZ += posZInverse;
            }

            state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
            apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }

    public static HitData traverseBlocks(GrimPlayer player, Vector3d start, Vector3d end, BiFunction<WrappedBlockState, Vector3i, HitData> predicate) {
        return traverseBlocks(player, new double[]{start.x, start.y, start.z}, new double[]{end.x, end.y, end.z}, predicate);
    }

    @Nullable
    public static BlockHitData getNearestHitResult(GrimPlayer player, double[] startPos, double[] lookVec, double currentDistance, double maxDistance, int[] targetBlockVec, BlockFace expectedBlockFace, SimpleCollisionBox[] boxes, boolean raycastContext) {
        double[] endPos = new double[]{
                startPos[0] + lookVec[0] * maxDistance,
                startPos[1] + lookVec[1] * maxDistance,
                startPos[2] + lookVec[2] * maxDistance
        };

        return (BlockHitData) traverseBlocks(player, startPos, endPos, (block, vector3i) -> {
            int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;

            List<WrappedBlockState> blockModifications =
                    player.blockHistory.getBlockStates((blockModification -> blockModification.location().equals(vector3i)
                            && currentTick - blockModification.tick() < 2
                            && (blockModification.cause() == BlockModification.Cause.START_DIGGING || blockModification.cause() == BlockModification.Cause.HANDLE_NETTY_SYNC_TRANSACTION)));
            blockModifications.add(0, block);

            BlockHitData hitData = null;
            boolean isTargetBlock = Arrays.equals(new int[]{vector3i.x, vector3i.y, vector3i.z}, targetBlockVec);
            for (WrappedBlockState wrappedBlockState : blockModifications) {
                hitData = didHitBlock(player, startPos, lookVec, currentDistance, maxDistance, targetBlockVec, expectedBlockFace, boxes, raycastContext, wrappedBlockState, vector3i);
                if (isTargetBlock) {
                    // target block, check if any possible block allows the ray to hit
                    if (hitData != null && hitData.success) {
                        return hitData;
                    }
                } else {
                    // non target block, we are checking if any possible block will allow the ray to pass through
                    if (hitData == null) {
                        return hitData;
                    }
                }
            }
            return hitData;
        });
    }

    public static BlockHitData didHitBlock(GrimPlayer player, double[] startPos, double[] lookVec, double currentDistance, double maxDistance, int[] targetBlockVec, BlockFace expectedBlockFace, SimpleCollisionBox[] boxes, boolean raycastContext, WrappedBlockState block, Vector3i vector3i) {
        CollisionBox data;
        boolean isTargetBlock = Arrays.equals(new int[]{vector3i.x, vector3i.y, vector3i.z}, targetBlockVec);
        if (!raycastContext) {
            data = HitboxData.getBlockHitbox(player, player.getInventory().getHeldItem().getType().getPlacedType(), player.getClientVersion(), block, isTargetBlock, vector3i.x, vector3i.y, vector3i.z);
        } else {
            data = RaycastData.getBlockHitbox(player, null, player.getClientVersion(), block, vector3i.x, vector3i.y, vector3i.z);
        }
        if (data == NoCollisionBox.INSTANCE) return null;
        int size = data.downCast(boxes);

        double bestHitResult = Double.MAX_VALUE;
        double[] bestHitLoc = null;
        BlockFace bestFace = null;

        double[] currentEnd = new double[]{
                startPos[0] + lookVec[0] * currentDistance,
                startPos[1] + lookVec[1] * currentDistance,
                startPos[2] + lookVec[2] * currentDistance
        };

        for (int i = 0; i < size; i++) {
            Pair<double[], BlockFace> intercept = ReachUtilsPrimitives.calculateIntercept(boxes[i], startPos, currentEnd);
            if (intercept.first() == null) continue; // No intercept or wrong blockFace

            double[] hitLoc = intercept.first();

            double distSq = distanceSquared(hitLoc, startPos);
            if (distSq < bestHitResult) {
                bestHitResult = distSq;
                bestHitLoc = hitLoc;
                bestFace = intercept.second();
                if (isTargetBlock && bestFace == expectedBlockFace) {
                    return new BlockHitData(vector3i, new Vector(bestHitLoc[0], bestHitLoc[1], bestHitLoc[2]), bestFace, block, true);
                }
            }
        }

        // Yes, this is not the most optimal algorithm for handling Cauldrons, Hoppers, Composters, and Scaffolding
        //   that is to say, blocks that have a different outline/hitbox shape from the box used to calculate placement blockfaces
        //   but it is the one vanilla uses
        // No we will not
        // 1. Calculate the ray trace from a new closer startPos to reduce iterations
        //    because it adds a lot of code complexity for very little performance gain
        // 2. Run a switch case on the target block and check if the index of the SimpleCollisionBox corresponds to a wall with an inside face
        //    and hardcode in blockface fixes for placements against those compnents above a certain relative y level
        //    because that is version-specific, will break if the implementation of the returned ComplexCollisionBox changes
        //    and again, lots of code complexity for little performance gain
        if (bestHitLoc != null) {
            BlockHitData hitData = new BlockHitData(vector3i, new Vector(bestHitLoc[0], bestHitLoc[1], bestHitLoc[2]), bestFace, block, isTargetBlock);
            if (!raycastContext) {
                BlockHitData hitData2 = BlockRayTrace.didHitBlock(player, startPos, lookVec, maxDistance, maxDistance, targetBlockVec, expectedBlockFace, boxes, true, block, vector3i);
                if (hitData2 != null) {
                    Vector startVector = new Vector(startPos[0], startPos[1], startPos[2]);
                    if (hitData2.getBlockHitLocation().subtract(startVector).lengthSquared() <
                            hitData.getBlockHitLocation().subtract(startVector).lengthSquared()) {
                        return new BlockHitData(vector3i, hitData.getBlockHitLocation(), hitData2.getClosestDirection(), block, isTargetBlock);
                    }
                }
            }
            return hitData;
        }

        return null;
    }

    private static double distanceSquared(double[] vec1, double[] vec2) {
        double dx = vec1[0] - vec2[0];
        double dy = vec1[1] - vec2[1];
        double dz = vec1[2] - vec2[2];
        return dx * dx + dy * dy + dz * dz;
    }

    public static HitData getNearestHitResult(GrimPlayer player, StateType heldItem, boolean sourcesHaveHitbox) {
        Vector3d startingPos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(player, startingPos.getX(), startingPos.getY(), startingPos.getZ(), player.xRot, player.yRot);
        final double distance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
        Vector endVec = trace.getPointAtDistance(distance);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());
        return getTraverseResult(player, heldItem, startingPos, startingVec, trace, endPos, sourcesHaveHitbox, false, distance + 3, false);
    }

    @Nullable
    public static HitData getNearestHitResult(GrimPlayer player, PacketEntity targetEntity, Vector eyePos, Vector lookVec, double currentDistance, boolean skipBlockCheck, boolean skipReachCheck) {

        double maxAttackDistance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
        double maxBlockDistance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_ENTITY_INTERACTION_RANGE);

        Vector3d startingPos = new Vector3d(eyePos.getX(), eyePos.getY(), eyePos.getZ());
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(eyePos, lookVec);
        Vector endVec = trace.getPointAtDistance(maxBlockDistance);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());

        // Get block hit
        HitData blockHitData = null;
        Vector closestHitVec = null;
        PacketEntity closestEntity = null;
        double closestDistanceSquared = Double.MAX_VALUE;
        if (!skipBlockCheck) {
            blockHitData = getTraverseResult(player, null, startingPos, startingVec, trace, endPos, false, true, currentDistance, true);
            closestDistanceSquared = blockHitData != null ? blockHitData.getBlockHitLocation().distanceSquared(startingVec) : maxAttackDistance * maxAttackDistance;
        }

        // Check entities
        if (!skipReachCheck) {
            for (PacketEntity entity : player.compensatedEntities.entityMap.values().stream().filter(TypedPacketEntity::canHit).collect(Collectors.toList())) {
                SimpleCollisionBox box = null;
                // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
                // Yes, this is vanilla and not uncertainty.  All reach checks have this or they are wrong.

                if (entity.equals(targetEntity)) {
                    box = entity.getPossibleCollisionBoxes();
                    box.expand(player.checkManager.getPacketCheck(Reach.class).reachThreshold);
                    // This is better than adding to the reach, as 0.03 can cause a player to miss their target
                    // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
                    //
                    // Just give the uncertainty on 1.9+ clients as we have no way of knowing whether they had 0.03 movement
                    if (!player.packetStateData.didLastLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9))
                        box.expand(player.getMovementThreshold());
                    if (ReachUtils.isVecInside(box, eyePos)) {
                        return new EntityHitData(entity, eyePos);
                    }
                } else {
                    CollisionBox b = entity.getMinimumPossibleCollisionBoxes();
                    if (b instanceof NoCollisionBox) {
                        continue;
                    } else {
                        box = (SimpleCollisionBox) b;
                    }
                    // todo, shrink by reachThreshold as well for non-target entities?
                    if (!player.packetStateData.didLastLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9))
                        box.expand(-player.getMovementThreshold());
                    if (ReachUtils.isVecInside(box, eyePos)) {
                        continue;
                    }
                }
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                    box.expand(0.1f);
                }


                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(Math.sqrt(closestDistanceSquared)));

                if (intercept.first() != null) {
                    double distSquared = intercept.first().distanceSquared(startingVec);
                    if (distSquared < closestDistanceSquared) {
                        closestDistanceSquared = distSquared;
                        closestHitVec = intercept.first();
                        closestEntity = entity;
                    }
                }
            }
        }

        return closestEntity == null ? blockHitData : new EntityHitData(closestEntity, closestHitVec);
    }

    // TODO replace shrinkBlocks boolean with a data structure/better way to represent
    // 1. We have a target block. Shrink everything by movementThreshold except expand target block (we are checking to see if it matches the target block)
    // 2. We do not have a target block. Shrink everything by movementThreshold()
    // 3. Do not expand or shrink everything, we do not expect 0.03/0.002 or we legacy example where we want to keep old behaviour
    private static HitData getTraverseResult(GrimPlayer player, @Nullable StateType heldItem, Vector3d startingPos, Vector startingVec, Ray trace, Vector3d endPos, boolean sourcesHaveHitbox, boolean checkInside, double knownDistance, boolean shrinkBlocks) {
        return traverseBlocks(player, startingPos, endPos, (block, vector3i) -> {
            // even though sometimes we are raytracing against a block that is the target block, we pass false to this function because it only applies a change for brewing stands in 1.8
            CollisionBox data = HitboxData.getBlockHitbox(player, heldItem, player.getClientVersion(), block, false, vector3i.getX(), vector3i.getY(), vector3i.getZ());
            SimpleCollisionBox[] boxes = new SimpleCollisionBox[ComplexCollisionBox.DEFAULT_MAX_COLLISION_BOX_SIZE];
            int size = data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            Vector bestHitLoc = null;
            BlockFace bestFace = null;

            for (int i = 0; i < size; i++) {
                if (shrinkBlocks) boxes[i].expand(-player.getMovementThreshold());
                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(boxes[i], trace.getOrigin(), trace.getPointAtDistance(knownDistance));
                if (intercept.first() == null) continue; // No intercept

                Vector hitLoc = intercept.first();

                // If inside a block, return empty result for reach check (don't bother checking this?)
                if (checkInside && ReachUtils.isVecInside(boxes[i], trace.getOrigin())) {
                    return null;
                }

                if (hitLoc.distanceSquared(startingVec) < bestHitResult) {
                    bestHitResult = hitLoc.distanceSquared(startingVec);
                    bestHitLoc = hitLoc;
                    bestFace = intercept.second();
                }
            }

            if (bestHitLoc != null) {
                return new BlockHitData(vector3i, bestHitLoc, bestFace, block, null);
            }

            if (sourcesHaveHitbox &&
                    (player.compensatedWorld.isWaterSourceBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ())
                            || player.compensatedWorld.getLavaFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ()) == (8 / 9f))) {
                double waterHeight = player.compensatedWorld.getFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ());
                SimpleCollisionBox box = new SimpleCollisionBox(vector3i.getX(), vector3i.getY(), vector3i.getZ(), vector3i.getX() + 1, vector3i.getY() + waterHeight, vector3i.getZ() + 1);

                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(knownDistance));

                if (intercept.first() != null) {
                    return new BlockHitData(vector3i, intercept.first(), intercept.second(), block, null);
                }
            }

            return null;
        });
    }
}
