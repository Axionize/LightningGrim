package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.CheckType;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PacketCheck extends AbstractCheck {
    default void onPacketReceive(final PacketReceiveEvent event) {}
    default void onPacketSend(final PacketSendEvent event) {}
    @Override
    default int getCheckMask() {
        return CheckType.PACKET.getMask();
    }
}
