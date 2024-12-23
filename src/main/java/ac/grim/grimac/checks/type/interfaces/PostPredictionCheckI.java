package ac.grim.grimac.checks.type.interfaces;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public interface PostPredictionCheckI extends PacketCheckI {

    default void onPredictionComplete(final PredictionComplete predictionComplete) {
    }

    @Override
    default int getMask() {
        return CheckType.POST_PREDICTION.getMask();
    }
}
