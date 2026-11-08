package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

@CheckData(name = "WrongBreak", stableKey = "grim.breaking.wrong_break", description = "Sent block break progress for a different block than the one being mined")
public class WrongBreak extends Check implements BlockBreakListener {
    private static final Verbose V =
            Verbose.of("action={digging}, last=[{mcpos}|null], pos={mcpos}");

    private final int exemptedY = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095);
    private boolean lastBlockWasInstantBreak = false;

    private boolean lastBlockIsSet;
    private int lastBlockX, lastBlockY, lastBlockZ;

    private boolean lastCancelledBlockIsSet;
    private int lastCancelledBlockX, lastCancelledBlockY, lastCancelledBlockZ;

    private boolean lastLastBlockIsSet;

    public WrongBreak(final GrimPlayer player) {
        super(player);
    }

    // The client sometimes sends a weird cancel packet
    private boolean shouldExempt(final WrappedBlockState block, int yPos) {
        if (lastLastBlockIsSet || !lastBlockIsSet)
            return false;

        // on pre 1.14.4 clients, the YPos of this packet is always the same
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) && yPos != exemptedY)
            return false;
        // and if this block is not an instant break
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)
                // getBlockDamage might not return the correct value if the player
                // switched slots before this, check all slots just to be safe
                || !BlockBreakSpeed.couldInstantlyBreakBlock(player, block.getType());
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        final int x = blockBreak.x;
        final int y = blockBreak.y;
        final int z = blockBreak.z;

        if (blockBreak.action == DiggingAction.START_DIGGING) {
            // getBlockDamage might not return the correct value if the player
            // switched slots before this, check all slots just to be safe
            lastBlockWasInstantBreak = BlockBreakSpeed.couldInstantlyBreakBlock(player, blockBreak.block.getType());
            lastCancelledBlockIsSet = false;

            lastLastBlockIsSet = lastBlockIsSet;

            lastBlockIsSet = true;
            lastBlockX = x;
            lastBlockY = y;
            lastBlockZ = z;
        } else if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            boolean notLastBlock = !lastBlockIsSet || x != lastBlockX || y != lastBlockY || z != lastBlockZ;

            if (!shouldExempt(blockBreak.block, y) && notLastBlock) {
                // https://github.com/GrimAnticheat/Grim/issues/1512
                boolean isLastCancelled = lastCancelledBlockIsSet && x == lastCancelledBlockX && y == lastCancelledBlockY && z == lastCancelledBlockZ;
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || (!lastBlockWasInstantBreak && isLastCancelled)) {
                    var buf = V.write(verbose()).uint(VerboseCodecs.enumId(DiggingAction.CANCELLED_DIGGING))
                            .bool(lastBlockIsSet)
                            .mcPos(lastBlockIsSet ? lastBlockX : 0, lastBlockIsSet ? lastBlockY : 0, lastBlockIsSet ? lastBlockZ : 0)
                            .mcPos(x, y, z);
                    if (flag(buf)) {
                        if (shouldModifyPackets()) {
                            blockBreak.cancel();
                        }
                    }
                }
            }

            lastCancelledBlockIsSet = true;
            lastCancelledBlockX = x;
            lastCancelledBlockY = y;
            lastCancelledBlockZ = z;

            lastLastBlockIsSet = lastBlockIsSet = false;
        } else if (blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            boolean notLastCancelled = !lastCancelledBlockIsSet || x != lastCancelledBlockX || y != lastCancelledBlockY || z != lastCancelledBlockZ;
            boolean notLastBlock = !lastBlockIsSet || x != lastBlockX || y != lastBlockY || z != lastBlockZ;

            // when a player looks away from the mined block, they send a cancel, and if they look at it again, they don't send another start. (thanks mojang!)
            if (notLastCancelled && (!lastBlockWasInstantBreak || player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) && notLastBlock) {
                var buf = V.write(verbose()).uint(VerboseCodecs.enumId(DiggingAction.FINISHED_DIGGING))
                        .bool(lastBlockIsSet)
                        .mcPos(lastBlockIsSet ? lastBlockX : 0, lastBlockIsSet ? lastBlockY : 0, lastBlockIsSet ? lastBlockZ : 0)
                        .mcPos(x, y, z);
                if (flag(buf)) {
                    if (shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                }
            }

            // 1.14.4+ clients don't send another start break in protected regions
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
                lastCancelledBlockIsSet = lastLastBlockIsSet = lastBlockIsSet = false;
            }
        }
    }
}
