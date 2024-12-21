package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.player.GrimPlayer;

public abstract class AbstractPrePredictionCheck extends AbstractPacketCheck {
    public AbstractPrePredictionCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getMask() {
        return CheckType.PRE_PREDICTION.getMask();
    }
}
