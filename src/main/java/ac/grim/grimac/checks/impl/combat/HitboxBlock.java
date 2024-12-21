package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "HitboxBlock", configName = "HitboxBlock", setback = 20)
public class HitboxBlock extends AbstractPacketCheck {
    public HitboxBlock(GrimPlayer player) {
        super(player);
    }
}
