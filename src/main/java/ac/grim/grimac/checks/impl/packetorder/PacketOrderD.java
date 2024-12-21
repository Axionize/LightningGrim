package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "PacketOrderD")
public class PacketOrderD extends AbstractPacketCheck {
    public PacketOrderD(GrimPlayer player) {
        super(player);
    }
}
