package ac.grim.grimac.checks.type.interfaces;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;

public interface PositionCheckI extends AbstractCheck {

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
    @Override
    default int getCheckMask() {
        return CheckType.POSITION.getMask();
    }
}
