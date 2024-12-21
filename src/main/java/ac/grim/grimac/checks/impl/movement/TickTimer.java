package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPrePredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;


@CheckData(name = "TickTimer", setback = 1)
public class TickTimer extends AbstractPrePredictionCheck {

    private boolean receivedTickEnd = true;
    private int flyingPackets = 0;

    public TickTimer(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.supportsEndTick()) return;
        if (isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasTeleport) {
            if (!receivedTickEnd) flagAndAlert("type=flying, packets=" + flyingPackets);
            receivedTickEnd = false;
            flyingPackets++;
        } else if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            receivedTickEnd = true;
            if (flyingPackets > 1) flagAndAlert("type=end, packets=" + flyingPackets);
            flyingPackets = 0;
        }
    }

    @Override
    public boolean supportsPlayer(GrimUser player) {
        // TODO make this a part of GrimUser interface and remove the server version check when we bypass via
        // TODO server version check when we bypass via for listening for packets
        return ((GrimPlayer) player).getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2);
    }
}
