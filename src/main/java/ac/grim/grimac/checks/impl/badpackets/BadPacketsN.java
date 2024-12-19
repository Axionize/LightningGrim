package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.interfaces.PacketCheckI;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPacketsN")
public class BadPacketsN extends Check implements PacketCheckI {
    public BadPacketsN(final GrimPlayer player) {
        super(player);
    }
}
