package ac.grim.grimac.checks.impl.sprint;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "SprintB", description = "Sprinting while sneaking", setback = 5, experimental = true)
public class SprintB extends Check implements PostPredictionCheck {
    public SprintB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (player.isSlowMovement && player.sneakingSpeedMultiplier < 0.8f) {
            ClientVersion version = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14_2) && version.isOlderThan(ClientVersion.V_1_21_4)) {
                return;
            }

            // https://github.com/GrimAnticheat/Grim/issues/1932
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14) && player.wasFlying && player.lastPose == Pose.FALL_FLYING && !player.isGliding) {
                return;
            }

            if (player.isSprinting && !player.isSwimming && (player.fluidOnEyes != FluidTag.WATER || version.isOlderThan(ClientVersion.V_1_21_4))) {
                flagAndAlertWithSetback();
            } else reward();
        }
    }
}
