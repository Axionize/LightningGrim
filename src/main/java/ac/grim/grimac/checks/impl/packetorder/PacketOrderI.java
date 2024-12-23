package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderI", experimental = true)
public class PacketOrderI extends AbstractPostPredictionCheck {
    public PacketOrderI(final GrimPlayer player) {
        super(player);
    }

    private boolean exemptPlacingWhileDigging;

    private boolean setback;
    private boolean digging; // for placing
    private final ArrayDeque<String> flags = new ArrayDeque<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                if (player.packetOrderProcessor.isRightClicking() || player.packetOrderProcessor.isPicking() || player.packetOrderProcessor.isReleasing() || player.packetOrderProcessor.isDigging()) {
                    if (!player.canSkipTicks()) {
                        if (flagAndAlert("attack") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        flags.add("attack");
                    }
                }
            } else if (player.packetOrderProcessor.isReleasing() || player.packetOrderProcessor.isDigging()) {
                if (!player.canSkipTicks()) {
                    if (flagAndAlert("interact") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add("interact");
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            if (player.packetOrderProcessor.isReleasing() || digging) {
                if (!player.canSkipTicks()) {
                    if (flagAndAlert("place/use") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add("place");
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            switch (packet.getAction()) {
                case RELEASE_USE_ITEM:
                    if (player.packetOrderProcessor.isAttacking() || player.packetOrderProcessor.isRightClicking() || player.packetOrderProcessor.isPicking() || player.packetOrderProcessor.isDigging()) {
                        if (!player.canSkipTicks()) {
                            if (flagAndAlert("release")) {
                                setback = true;
                            }
                        } else {
                            flags.add("release");
                            setback = true;
                        }
                    }
                    break;
                case START_DIGGING:
                    double damage = BlockBreakSpeed.getBlockDamage(player, packet.getBlockPosition());
                    if (damage >= 1 || damage <= 0 && player.gamemode == GameMode.CREATIVE) {
                        return;
                    }
                case CANCELLED_DIGGING:
                case FINISHED_DIGGING:
                    if (exemptPlacingWhileDigging || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                        return;
                    }
                    digging = true;
            }
        }

        if (isTickPacket(event.getPacketType())) {
            digging = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (setback) {
                setbackIfAboveSetbackVL();
                setback = false;
            }
            return;
        }

        if (player.isTickingReliablyFor(3) && !player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(0)) {
            for (String verbose : flags) {
                if (flagAndAlert(verbose) && setback) {
                    setbackIfAboveSetbackVL();
                    setback = false;
                }
            }
        }

        flags.clear();
        setback = false;
    }

    @Override
    public void onReload(ConfigManager config) {
        exemptPlacingWhileDigging = config.getBooleanElse(getConfigName() + ".exempt-placing-while-digging", false);
    }
}
