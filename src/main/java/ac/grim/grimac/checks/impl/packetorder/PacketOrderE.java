package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "PacketOrderE", experimental = true)
public class PacketOrderE extends AbstractPostPredictionCheck {
    public PacketOrderE(final GrimPlayer player) {
        super(player);
    }

    private int invalid;
    private boolean setback;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            if (player.packetOrderProcessor.isAttacking()
                    || player.packetOrderProcessor.isRightClicking()
                    || player.packetOrderProcessor.isOpeningInventory()
                    || player.packetOrderProcessor.isReleasing()
                    || player.packetOrderProcessor.isSneaking()
                    || player.packetOrderProcessor.isSprinting()
                    || player.packetOrderProcessor.isLeavingBed()
                    || player.packetOrderProcessor.isStartingToGlide()
                    || player.packetOrderProcessor.isJumpingWithMount()
            ) {
                if (player.canSkipTicks() || flagAndAlert()) {
                    invalid++;

                    if (player.packetOrderProcessor.isUsing()) {
                        setback = true;
                    }
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (setback) {
                setback = false;
                setbackIfAboveSetbackVL();
            }
            return;
        }

        if (player.isTickingReliablyFor(3) && !player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(0)) {
            for (; invalid >= 1; invalid--) {
                if (flagAndAlert() && setback) {
                    setback = false;
                    setbackIfAboveSetbackVL();
                }
            }
        }

        invalid = 0;
    }
}
