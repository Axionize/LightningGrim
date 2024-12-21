package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsN")
public class BadPacketsN extends AbstractPacketCheck {
    public BadPacketsN(final GrimPlayer player) {
        super(player);
    }
}
