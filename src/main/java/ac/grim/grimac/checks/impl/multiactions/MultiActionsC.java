package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "MultiActionsC", description = "Clicked in inventory while performing other actions", experimental = true)
public class MultiActionsC extends AbstractPacketCheck {
    public MultiActionsC(GrimPlayer player) {
        super(player);
    }

    private boolean serverOpenedInventoryThisTick;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            String verbose = player.isSprinting && !player.isSwimming && !serverOpenedInventoryThisTick ? "sprinting" : "";

            if (player.packetStateData.isSlowedByUsingItem()) {
                verbose += (verbose.isEmpty() ? "" : ", ") + "using";
            }

            if (!verbose.isEmpty() && flagAndAlert(verbose) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        if (isTickPacket(event.getPacketType())) {
            serverOpenedInventoryThisTick = false;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> serverOpenedInventoryThisTick = true);
        }
    }
}
