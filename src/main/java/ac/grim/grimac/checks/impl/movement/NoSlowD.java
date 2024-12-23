package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "NoSlowD", description = "Sprinting while using an item", setback = 5, experimental = true)
public class NoSlowD extends AbstractPostPredictionCheck {
    public NoSlowD(GrimPlayer player) {
        super(player);
    }

    public boolean startedSprintingBeforeUse = false;
    private boolean flaggedLastTick = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            if (new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
                startedSprintingBeforeUse = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (player.packetStateData.isSlowedByUsingItem()) {
            ClientVersion client = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (startedSprintingBeforeUse && client.isNewerThanOrEquals(ClientVersion.V_1_14_2) && client.isOlderThan(ClientVersion.V_1_21_4)) {
                reward();
                flaggedLastTick = false;
                return;
            }

            if (player.isSprinting && (!player.isSwimming || client.isOlderThan(ClientVersion.V_1_21_4))) {
                if (flaggedLastTick && flagWithSetback()) alert("");
                flaggedLastTick = true;
            } else {
                reward();
                flaggedLastTick = false;
            }
        }
    }
}
