package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public interface PostPredictionCheck extends PacketCheck {

    default void onPredictionComplete(final PredictionComplete predictionComplete) {
    }
    @Override
    default int getCheckMask() {
        return CheckType.POST_PREDICTION.getMask();
    }
}
