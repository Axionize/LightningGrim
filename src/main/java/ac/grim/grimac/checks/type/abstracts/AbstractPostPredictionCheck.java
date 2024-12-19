package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.PostPredictionCheckI;
import ac.grim.grimac.player.GrimPlayer;

public class AbstractPostPredictionCheck extends Check implements PostPredictionCheckI {
    public AbstractPostPredictionCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getCheckMask() {
        return CheckType.POST_PREDICTION.getMask();
    }
}
