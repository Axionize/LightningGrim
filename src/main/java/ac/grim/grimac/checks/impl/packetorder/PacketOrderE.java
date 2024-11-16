package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "PacketOrderE", experimental = true)
public class PacketOrderE extends Check implements PostPredictionCheck {
    public PacketOrderE(final GrimPlayer player) {
        super(player);
    }

    private int invalid = 0;
    private boolean sent = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            if (sent || player.packetOrderProcessor.isAttacking() || player.packetOrderProcessor.isRightClicking() || player.packetOrderProcessor.isOpeningInventory() || player.packetOrderProcessor.isSwapping() || player.packetOrderProcessor.isDropping() || player.packetOrderProcessor.isReleasing()) {
                if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8) || flagAndAlert()) {
                    invalid++;
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            sent = true;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && !player.packetStateData.lastPacketWasTeleport) {
            sent = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            if (invalid > 0) {
                setbackIfAboveSetbackVL();
            }

            invalid = 0;
            return;
        }

        if (!player.skippedTickInActualMovement && predictionComplete.isChecked()) {
            for (; invalid >= 1; invalid--) {
                if (flagAndAlert()) {
                    setbackIfAboveSetbackVL();
                }
            }
        }

        invalid = 0;
        sent = false;
    }
}
