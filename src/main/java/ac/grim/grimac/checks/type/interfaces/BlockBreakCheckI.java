package ac.grim.grimac.checks.type.interfaces;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;

public interface BlockBreakCheckI extends PostPredictionCheckI {
    default void onBlockBreak(final BlockBreak blockBreak) {}

    @Override
    default int getMask() {
        return CheckType.BLOCK_BREAK.getMask();
    }
}
