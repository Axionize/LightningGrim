package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.BlockBreakCheckI;
import ac.grim.grimac.player.GrimPlayer;

public class AbstractBlockBreakCheck extends Check implements BlockBreakCheckI {
    public AbstractBlockBreakCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getCheckMask() {
        return CheckType.BLOCK_BREAK.getMask();
    }
}
