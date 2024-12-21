package ac.grim.grimac.checks.type.interfaces;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;

public interface BlockPlaceCheckI extends PostPredictionCheckI {
    void onBlockPlace(final BlockPlace place);

    void onPostFlyingBlockPlace(BlockPlace place);

    @Override
    default int getCheckMask() {
        return CheckType.BLOCK_PLACE.getMask();
    }
}
