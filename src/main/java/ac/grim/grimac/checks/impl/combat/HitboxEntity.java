package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

public class HitboxEntity extends Check implements PacketCheck {
    public HitboxEntity(GrimPlayer player) {
        super(player);
    }
}
