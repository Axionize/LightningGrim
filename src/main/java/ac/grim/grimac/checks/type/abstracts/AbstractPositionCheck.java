package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.PositionCheckI;
import ac.grim.grimac.player.GrimPlayer;

public abstract class AbstractPositionCheck extends Check implements PositionCheckI {
    public AbstractPositionCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getMask() {
        return CheckType.POSITION.getMask();
    }
}
