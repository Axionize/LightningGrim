package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "HitboxBlock", configName = "HitboxBlock", setback = 20)
public class HitboxBlock extends Check implements PacketCheck {
    public HitboxBlock(GrimPlayer player) {
        super(player);
    }
}
