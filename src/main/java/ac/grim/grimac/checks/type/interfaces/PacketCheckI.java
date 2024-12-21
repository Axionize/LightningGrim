package ac.grim.grimac.checks.type.interfaces;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.CheckType;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PacketCheckI extends AbstractCheck {
    default void onPacketReceive(final PacketReceiveEvent event) {}
    default void onPacketSend(final PacketSendEvent event) {}
    @Override
    default int getMask() {
        return CheckType.PACKET.getMask();
    }
}
