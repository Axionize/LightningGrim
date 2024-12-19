package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.RotationCheckI;
import ac.grim.grimac.player.GrimPlayer;

public class AbstractRotationCheck extends Check implements RotationCheckI {
    public AbstractRotationCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getCheckMask() {
        return CheckType.ROTATION.getMask();
    }
}
