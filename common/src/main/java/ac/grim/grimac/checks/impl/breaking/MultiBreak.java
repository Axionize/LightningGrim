package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiBreak", stableKey = "grim.breaking.multi_break", description = "Tried to break multiple different blocks in the same movement tick", experimental = true)
public class MultiBreak extends Check implements BlockBreakListener, PreViaPacketReceiveListener, PostPredictionListener {
    private static final Verbose V =
            Verbose.of("face={face}, lastFace={face}, pos={mcpos}, lastPos={mcpos}");

    private final List<FlagData> flags = new ArrayList<>();
    private boolean hasBroken;
    private BlockFace lastFace;
    private int lastX;
    private int lastY;
    private int lastZ;

    public MultiBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            return;
        }

        final int currentX = blockBreak.x;
        final int currentY = blockBreak.y;
        final int currentZ = blockBreak.z;

        if (hasBroken && (blockBreak.face != lastFace ||
                currentX != lastX ||
                currentY != lastY ||
                currentZ != lastZ)) {
            final int face = VerboseCodecs.enumId(blockBreak.face);
            final int previousFace = VerboseCodecs.enumId(lastFace);
            if (!player.canSkipTicks()) {
                var buf = V.write(verbose()).uint(face).uint(previousFace)
                        .mcPos(currentX, currentY, currentZ)
                        .mcPos(lastX, lastY, lastZ);
                if (flag(buf) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                flags.add(new FlagData(face, previousFace, currentX, currentY, currentZ, lastX, lastY, lastZ));
            }
        }

        lastFace = blockBreak.face;
        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        hasBroken = true;
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            hasBroken = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flag(V.write(verbose()).uint(data.face()).uint(data.previousFace())
                        .mcPos(data.x(), data.y(), data.z())
                        .mcPos(data.previousX(), data.previousY(), data.previousZ()));
            }
        }

        flags.clear();
    }

    private record FlagData(int face, int previousFace, int x, int y, int z, int previousX, int previousY, int previousZ) {}
}
