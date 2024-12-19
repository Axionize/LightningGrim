package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.interfaces.PacketCheckI;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "HitboxMiss", configName = "HitboxMiss", setback = 10)
public class HitboxMiss extends Check implements PacketCheckI {
    public HitboxMiss(GrimPlayer player) {
        super(player);
    }
}
