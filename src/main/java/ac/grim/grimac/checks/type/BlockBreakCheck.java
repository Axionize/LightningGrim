package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;

public interface BlockBreakCheck extends PostPredictionCheck {
    default void onBlockBreak(final BlockBreak blockBreak) {}

    @Override
    default int getCheckMask() {
        return CheckType.BLOCK_PLACE.getMask();
    }
}
