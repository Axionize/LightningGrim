package ac.grim.grimac.predictionengine.predictions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWaterLegacy extends PredictionEngine {

    private float swimmingFriction;

    public void guessBestMovement(float swimmingSpeed, GrimPlayer player, float swimmingFriction) {
        this.swimmingFriction = swimmingFriction;
        super.guessBestMovement(swimmingSpeed, player);
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        final VectorData[] vectorsToProcess = existingVelocities.toArray(new VectorData[0]);
        for (VectorData vector : vectorsToProcess) {
            existingVelocities.add(new VectorData(vector.vectorX, vector.vectorY + 0.04f, vector.vectorZ, vector, VectorData.VectorType.Jump));

            if (player.skippedTickInActualMovement) {
                existingVelocities.add(new VectorData(vector.vectorX,vector.vectorY + 0.02f, vector.vectorZ, vector, VectorData.VectorType.Jump));
            }
        }
    }

    @Override
    public void endOfTick(GrimPlayer player, double playerGravity) {
        super.endOfTick(player, playerGravity);

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vectorX *= swimmingFriction;
            vector.vectorY *= 0.8F;
            vector.vectorZ *= swimmingFriction;

            // Gravity
            vector.vectorY -= 0.02D;
        }
    }
}
