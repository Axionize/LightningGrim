package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

@CheckData(name = "MultiActionsE", description = "Swinging while using an item", experimental = true)
public class MultiActionsE extends AbstractPacketCheck {
    public MultiActionsE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.packetStateData.isSlowedByUsingItem() && (player.packetStateData.lastSlotSelected == player.packetStateData.getSlowedByUsingItemSlot() || player.packetStateData.eatingHand == InteractionHand.OFF_HAND) && event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            // this is possible to false on 1.7
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                return;
            }

            if (flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }

    @Override
    public boolean supportsPlayer(GrimUser player) {
        // TODO make this a part of GrimUser interface
        // TODO server version check when we bypass via for listening for packets
        return ((GrimPlayer) player).getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8);
    }
}
