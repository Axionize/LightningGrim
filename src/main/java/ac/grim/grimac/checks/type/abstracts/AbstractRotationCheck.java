package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.RotationCheckI;
import ac.grim.grimac.player.GrimPlayer;

public abstract class AbstractRotationCheck extends Check implements RotationCheckI {
    public AbstractRotationCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getMask() {
        return CheckType.ROTATION.getMask();
    }
}
