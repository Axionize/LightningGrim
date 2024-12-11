package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "HitboxMiss", configName = "HitboxMiss", setback = 10)
public class HitboxMiss extends Check implements PacketCheck {
    public HitboxMiss(GrimPlayer player) {
        super(player);
    }
}
