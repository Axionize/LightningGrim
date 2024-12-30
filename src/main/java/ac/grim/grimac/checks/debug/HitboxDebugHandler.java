package ac.grim.grimac.checks.debug;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handles debug visualization of hitboxes and reach calculations for GrimAC.
 * Sends debug data to clients through plugin messages that can be visualized by compatible clients.
 */
public class HitboxDebugHandler extends AbstractDebugHandler {

    /**
     * Set of players currently listening to hitbox debug data
     */
    private final Set<Player> listeners = new CopyOnWriteArraySet<>(new HashSet<>());

    /**
     * Creates a new HitboxDebugHandler for the specified player
     *
     * @param grimPlayer The GrimAC player instance to debug
     */
    public HitboxDebugHandler(GrimPlayer grimPlayer) {
        super(grimPlayer);
    }

    /**
     * Toggles whether a player receives hitbox debug visualization data.
     * If the player is already listening, they will be removed. If not, they will be added.
     *
     * @param player The player to toggle debug visualization for
     */
    @Override
    public void toggleListener(Player player) {
        if (!listeners.remove(player)) listeners.add(player);
    }

    @Override
    public boolean toggleConsoleOutput() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sends debug visualization data for reach calculations to all listening players.
     * The data includes hitboxes, target entities, look vectors, and eye heights used in reach calculations.
     *
     * @param hitboxes Map of entity IDs to their collision boxes
     * @param targetEntities Set of entity IDs that are considered targets
     * @param lookVecsAndEyeHeights List of pairs containing look vectors and their corresponding eye heights
     * @param basePos Base position before eye height adjustments
     * @param isPrediction Whether these hitboxes are from a prediction calculation
     *
     * Packet Format (Version 1):
     * - Byte: Version (0)
     * - Byte: Global flags
     *   - Bit 0: isPrediction
     *   - Bits 1-7: Reserved
     * - Double: Player reach/interact distance
     * - Vector3d: Base position (3 doubles)
     * - VarInt: Number of ray traces
     * - For each ray trace:
     *   - Double: Eye height delta
     *   - Vector3d: Look vector (3 doubles)
     * - VarInt: Number of hitboxes
     * - For each hitbox:
     *   - VarInt: Entity ID
     *   - Byte: Flags
     *     - Bit 0: Is target entity
     *     - Bit 1: Is no collision
     *     - Bits 2-7: Reserved
     *   - If not NoCollisionBox:
     *     - Double: minX
     *     - Double: minY
     *     - Double: minZ
     *     - Double: maxX
     *     - Double: maxY
     *     - Double: maxZ
     */
    public void sendHitboxData(Map<Integer, CollisionBox> hitboxes, Set<Integer> targetEntities,
                               List<Pair<Vector, Double>> lookVecsAndEyeHeights, Vector basePos,
                               boolean isPrediction, double reachDistance) {
        if (listeners.isEmpty()) return;

        ByteBuf buffer = Unpooled.buffer();
        try {
            // Version Header
            buffer.writeByte(1);

            // Global Flags Header
            // Pack boolean flags into a single byte
            byte global_flags = 0;
            global_flags |= (isPrediction ? 1 : 0);     // Bit 0: are the hitboxes from a prediction?
            // Bits 2-7 reserved for future use
            buffer.writeByte(global_flags);

            // Write reach distance
            buffer.writeDouble(reachDistance);

            // Write base position
            writeVector(buffer, basePos);

            // Write number of ray traces
            ByteBufHelper.writeVarInt(buffer, lookVecsAndEyeHeights.size());

            // Write all possible ray traces
            for (Pair<Vector, Double> pair : lookVecsAndEyeHeights) {
                Vector lookVec = pair.first();
                double eyeHeight = pair.second();

                // Write eye height delta and look vector
                buffer.writeDouble(eyeHeight);
                writeVector(buffer, lookVec);
            }

            // Write number of hitboxes
            ByteBufHelper.writeVarInt(buffer, hitboxes.size());

            // Write hitbox data
            for (Map.Entry<Integer, CollisionBox> entry : hitboxes.entrySet()) {
                int entityId = entry.getKey();
                CollisionBox box = entry.getValue();

                // Write entity ID
                ByteBufHelper.writeVarInt(buffer, entityId);

                // Pack boolean flags into a single byte
                byte flags = 0;
                flags |= (targetEntities.contains(entityId) ? 1 : 0);     // Bit 0: Is target
                flags |= (box == NoCollisionBox.INSTANCE ? 2 : 0);        // Bit 1: Is no collision
                // Bits 2-7 reserved for future use
                buffer.writeByte(flags);

                // Write box coordinates if it's not a NoCollisionBox
                if ((flags & 2) == 0) {
                    SimpleCollisionBox simpleCollisionBox = (SimpleCollisionBox) box;
                    buffer.writeDouble(simpleCollisionBox.minX);
                    buffer.writeDouble(simpleCollisionBox.minY);
                    buffer.writeDouble(simpleCollisionBox.minZ);
                    buffer.writeDouble(simpleCollisionBox.maxX);
                    buffer.writeDouble(simpleCollisionBox.maxY);
                    buffer.writeDouble(simpleCollisionBox.maxZ);
                }
            }

            // Convert buffer to byte array
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);

            // Create and send packet
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(
                    "grim:debug_hitbox",
                    data
            );

            // Send to all listeners
            for (Player listener : listeners) {
                if (listener != null) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(listener, packet);
                }
            }
        } finally {
            // Release buffer to prevent memory leaks
            buffer.release();
        }
    }

    /**
     * Helper method to write a Vector to the ByteBuf
     * @param buffer The buffer to write to
     * @param vector The vector to write
     */
    private void writeVector(ByteBuf buffer, Vector vector) {
        buffer.writeDouble(vector.getX());
        buffer.writeDouble(vector.getY());
        buffer.writeDouble(vector.getZ());
    }
}
