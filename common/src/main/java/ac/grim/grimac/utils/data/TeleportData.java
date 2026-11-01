package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Getter
public class TeleportData {
    private final double locationX;
    private final double locationY;
    private final double locationZ;
    private final float yaw;
    private final float pitch;
    private final double velocityX;
    private final double velocityY;
    private final double velocityZ;
    // false = no velocity (setbacks, clients below 1.21.2); velocityX/Y/Z are ignored then
    private final boolean hasVelocity;
    private final @NotNull RelativeFlag flags;
    @Setter
    private int transaction;
    @Setter
    private int teleportId;

    public boolean isValidRotation(ClientVersion version, float receivedYaw, float receivedPitch) {
        // before 1.19.3, the teleport handler uses setPositionAndRotation / absMoveTo
        float expectedYaw = version.isOlderThan(ClientVersion.V_1_19_3) ? yaw % 360.0F : yaw;
        float expectedPitch = pitch;
        if (version.isOlderThanOrEquals(ClientVersion.V_1_8)) {
            expectedPitch %= 360.0F; // 1.8 applies setRotation without first clamping the pitch
        } else if (version.isOlderThan(ClientVersion.V_1_19_3) || version.isNewerThanOrEquals(ClientVersion.V_1_21_5)) {
            expectedPitch = GrimMath.clamp(expectedPitch, -90.0F, 90.0F); // 1.9-1.19.2 clamp in absMoveTo, since 1.21.5, calculateAbsolute clamps first
        } else if (version.isNewerThanOrEquals(ClientVersion.V_1_21_2)) {
            expectedPitch = GrimMath.clamp(expectedPitch % 360.0F, -90.0F, 90.0F); // 1.21.2-1.21.4 only normalize in setXRot, so the remainder precedes the clamp
        }

        // rotations are updated every frame, we can't accurately check them if they're relative
        return (isRelativeYaw() || receivedYaw == expectedYaw) && (isRelativePitch() || receivedPitch == expectedPitch);
    }

    public void modifyVector(@NotNull GrimPlayer player, Vector3dm vector) {
        final boolean isStupidTeleportSystem = player.supportsEndTick();
        if (!isStupidTeleportSystem) {
            if (!isRelativeX()) {
                vector.setX(0);
            }

            if (!isRelativeY()) {
                vector.setY(0);
                player.lastWasClimbing = 0; // Vertical movement reset
                player.canSwimHop = false; // Vertical movement reset
            }

            if (!isRelativeZ()) {
                vector.setZ(0);
            }
        } else if (hasVelocity) {
            // WHAT WAS MOJANG THINKING MAKING TELEPORTS A REPLACEMENT PACKET FOR EXPLOSION VELOCITY /s
            if (isRelativeDeltaX()) {
                vector.setX(vector.getX() + velocityX);
            } else {
                vector.setX(velocityX);
            }

            if (isRelativeDeltaY()) {
                vector.setY(vector.getY() + velocityY);
            } else {
                vector.setY(velocityY);
                // Is this correct? Don't know don't care.
                player.lastWasClimbing = 0; // Vertical movement reset
                player.canSwimHop = false; // Vertical movement reset
            }

            if (isRelativeDeltaZ()) {
                vector.setZ(vector.getZ() + velocityZ);
            } else {
                vector.setZ(velocityZ);
            }
        }
    }

    public boolean isRelativeVelocity() {
        return isRelativeDeltaX() || isRelativeDeltaY() || isRelativeDeltaZ();
    }

    public boolean isRelativeDeltaX() {
        return flags.has(RelativeFlag.DELTA_X);
    }

    public boolean isRelativeDeltaY() {
        return flags.has(RelativeFlag.DELTA_Y);
    }

    public boolean isRelativeDeltaZ() {
        return flags.has(RelativeFlag.DELTA_Z);
    }

    public boolean isRelativePos() {
        return isRelativeX() || isRelativeY() || isRelativeZ();
    }

    public boolean isRelativeX() {
        return flags.has(RelativeFlag.X);
    }

    public boolean isRelativeY() {
        return flags.has(RelativeFlag.Y);
    }

    public boolean isRelativeZ() {
        return flags.has(RelativeFlag.Z);
    }

    public boolean isRelativeYaw() {
        return flags.has(RelativeFlag.YAW);
    }

    public boolean isRelativePitch() {
        return flags.has(RelativeFlag.PITCH);
    }
}
