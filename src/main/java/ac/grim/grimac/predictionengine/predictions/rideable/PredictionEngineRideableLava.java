package ac.grim.grimac.predictionengine.predictions.rideable;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineLava;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.math.Vector3dm;

import java.util.List;
import java.util.Set;

public class PredictionEngineRideableLava extends PredictionEngineLava {

    Vector3dm movementVector;

    public PredictionEngineRideableLava(Vector3dm movementVector) {
        this.movementVector = movementVector;
    }

    @Override
    public void addJumpsToPossibilities(GrimPlayer player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(GrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(movementVector, player, possibleVectors, speed);
    }
}
