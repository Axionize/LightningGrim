package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.interfaces.PacketCheckI;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "HitboxEntity", configName = "HitboxEntity", setback = 30)
public class HitboxEntity extends Check implements PacketCheckI {
    public HitboxEntity(GrimPlayer player) {
        super(player);
    }
}
