package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

@UtilityClass
public class JumpPower {
    public static void jumpFromGround(@NotNull GrimPlayer player, @NotNull double[] vectorArr) {
        float jumpPower = getJumpPower(player);

        final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
            double jumpVelocity = jumpPower;
            if (jumpBoost.isPresent()) {
                jumpVelocity += (jumpBoost.getAsInt() + 1) * 0.1F;
            }
            // Directly set the Y-value in the array.
            vectorArr[1] = jumpVelocity;
        } else {
            if (jumpBoost.isPresent()) {
                jumpPower += 0.1f * (jumpBoost.getAsInt() + 1);
            }

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5) && jumpPower <= 1.0E-5f) {
                return;
            }

            // Directly set the Y-value in the array.
            vectorArr[1] = player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2) ? jumpPower : Math.max(jumpPower, vectorArr[1]);
        }

        if (player.isSprinting) {
            float radRotation = GrimMath.radians(player.yaw);
            // Below 1.20.5 this is float * float, so the 0.2F can't be widened to a double first
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
                vectorArr[0] += -player.trigHandler.sin(radRotation) * 0.2;
                vectorArr[2] += player.trigHandler.cos(radRotation) * 0.2;
            } else {
                vectorArr[0] += -player.trigHandler.sin(radRotation) * 0.2F;
                vectorArr[2] += player.trigHandler.cos(radRotation) * 0.2F;
            }
        }
    }

    public static float getJumpPower(@NotNull GrimPlayer player) {
        float jumpStrength = player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5)
                ? 0.42F
                : (float) player.compensatedEntities.self.getAttributeValue(Attributes.JUMP_STRENGTH);
        return jumpStrength * getPlayerJumpFactor(player);
    }

    public static float getPlayerJumpFactor(@NotNull GrimPlayer player) {
        return BlockProperties.onHoneyBlock(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ)) ? 0.5f : 1f;
    }
}
