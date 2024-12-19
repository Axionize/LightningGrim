package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.PacketCheckI;
import ac.grim.grimac.player.GrimPlayer;

public class AbstractPrePredictionCheck extends Check implements PacketCheckI {
    public AbstractPrePredictionCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getCheckMask() {
        return CheckType.PRE_PREDICTION.getMask();
    }
}
