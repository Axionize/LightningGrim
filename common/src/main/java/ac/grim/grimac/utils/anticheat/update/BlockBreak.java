package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BlockBreak {
    public final int x;
    public final int y;
    public final int z;
    public final @NotNull BlockFace face;
    public final int faceId;
    public final @NotNull DiggingAction action;
    public final int sequence;
    public final @NotNull WrappedBlockState block;
    private final @NotNull GrimPlayer player;
    @Getter
    private boolean cancelled;

    public BlockBreak(@NotNull GrimPlayer player,
                      final int x,
                      final int y,
                      final int z,
                      @NotNull BlockFace face,
                      int faceId,
                      @NotNull DiggingAction action,
                      int sequence,
                      @NotNull WrappedBlockState block) {
        this.player = Objects.requireNonNull(player, "player");
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = Objects.requireNonNull(face, "face");
        this.faceId = faceId;
        this.action = Objects.requireNonNull(action, "action");
        this.sequence = sequence;
        this.block = Objects.requireNonNull(block, "block");
    }

    @Contract(mutates = "this")
    public void cancel() {
        this.cancelled = true;
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull SimpleCollisionBox getCombinedBox() {
        CollisionBox placedOn = HitboxData.getBlockHitbox(player, player.inventory.getHeldItem().getType().getPlacedType(), player.getClientVersion(), block, true, x, y, z);

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        placedOn.downCast(boxes);

        SimpleCollisionBox combined = new SimpleCollisionBox(x, y, z);
        for (SimpleCollisionBox box : boxes) {
            double minX = Math.max(box.minX, combined.minX);
            double minY = Math.max(box.minY, combined.minY);
            double minZ = Math.max(box.minZ, combined.minZ);
            double maxX = Math.min(box.maxX, combined.maxX);
            double maxY = Math.min(box.maxY, combined.maxY);
            double maxZ = Math.min(box.maxZ, combined.maxZ);
            combined = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        return combined;
    }
}
