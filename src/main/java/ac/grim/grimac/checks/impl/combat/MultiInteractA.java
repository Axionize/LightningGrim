package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPostPredictionCheck;
import ac.grim.grimac.checks.type.interfaces.PostPredictionCheckI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayList;

@CheckData(name = "MultiInteractA", description = "Interacted with multiple entities in the same tick", experimental = true)
public class MultiInteractA extends AbstractPostPredictionCheck {
    public MultiInteractA(final GrimPlayer player) {
        super(player);
    }

    private final ArrayList<String> flags = new ArrayList<>();
    private int lastEntity;
    private boolean lastSneaking;
    private boolean hasInteracted = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            int entity = packet.getEntityId();
            boolean sneaking = packet.isSneaking().orElse(false);

            if (hasInteracted && entity != lastEntity) {
                String verbose = "lastEntity=" + lastEntity + ", entity=" + entity
                        + ", lastSneaking=" + lastSneaking + ", sneaking=" + sneaking;
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }

            lastEntity = entity;
            lastSneaking = sneaking;
            hasInteracted = true;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && !player.packetStateData.lastPacketWasTeleport) {
            hasInteracted = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        // we don't need to check pre-1.9 players here (no tick skipping)
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

        if (player.isTickingReliablyFor(3) && !player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(0)) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
        hasInteracted = false;
    }
}
