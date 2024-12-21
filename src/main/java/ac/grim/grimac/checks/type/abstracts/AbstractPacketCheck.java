package ac.grim.grimac.checks.type.abstracts;

import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.PacketCheckI;
import ac.grim.grimac.player.GrimPlayer;

public abstract class AbstractPacketCheck extends Check implements PacketCheckI {
    public AbstractPacketCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public int getMask() {
        return CheckType.PACKET.getMask();
    }
}
