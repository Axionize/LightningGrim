package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import ac.grim.grimac.utils.inventory.InventoryDesyncStatus;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus.Action;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;

public class PacketPlayerWindow extends PacketListenerAbstract {

    public PacketPlayerWindow() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !event.isCancelled()) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if (player.hasInventoryOpen && isNearNetherPortal(player)) {
                handleInventoryClose(player, InventoryDesyncStatus.NETHER_PORTAL);
            }
        }

        // Client Status is sent in 1.7-1.8
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            WrapperPlayClientClientStatus wrapper = new WrapperPlayClientClientStatus(event);

            if (wrapper.getAction() == Action.OPEN_INVENTORY_ACHIEVEMENT) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                handleInventoryOpen(player);
            }
        }

        // We need to do this due to 1.9 not sending anymore the inventory action in the Client Status
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            // TODO: Remove this check after we finish the before ViaVersion injection
            // Explanation: On 1.7 and 1.8 we have OPEN_INVENTORY_ACHIEVEMENT on CLIENT_STATUS packet
            // but after Via translation this information gets lost
            // This is a workaround to atleast make our inventory checks work "decently" in 1.8 clients for 1.9+ servers
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
                    && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                handleInventoryOpen(player);
            }

            if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8)) {
                handleInventoryOpen(player);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            handleInventoryClose(player, InventoryDesyncStatus.NOT_DESYNCED);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> handleInventoryClose(player, InventoryDesyncStatus.NOT_DESYNCED));
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow wrapper = new WrapperPlayServerOpenWindow(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            String legacyType = wrapper.getLegacyType();
            int modernType = wrapper.getType();
            InventoryDesyncStatus inventoryDesyncStatus = getContainerDesyncStatus(player, legacyType, modernType);

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> {
                        if (inventoryDesyncStatus == InventoryDesyncStatus.NOT_DESYNCED) {
                            handleInventoryOpen(player);
                        } else {
                            handleInventoryClose(player, inventoryDesyncStatus);
                        }
                    });
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> handleInventoryOpen(player));
        } else if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> handleInventoryClose(player, InventoryDesyncStatus.NOT_DESYNCED));
        }
    }

    private void handleInventoryOpen(GrimPlayer player) {
        if (!player.hasInventoryOpen) {
            player.lastInventoryOpen = System.currentTimeMillis();
        }

        player.hasInventoryOpen = true;
    }

    private void handleInventoryClose(GrimPlayer player, InventoryDesyncStatus desyncStatus) {
        player.hasInventoryOpen = false;
        player.inventoryDesyncStatus = desyncStatus;
    }

    public InventoryDesyncStatus getContainerDesyncStatus(GrimPlayer player, String legacyType, int modernType) {
        // Closing beacon with the cross button cause desync in 1.7-1.8
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) &&
                ("minecraft:beacon".equals(legacyType) || modernType == 8)) {
            return player.inventoryDesyncStatus = InventoryDesyncStatus.BEACON;
        }

        if (isNearNetherPortal(player)) {
            return player.inventoryDesyncStatus = InventoryDesyncStatus.NETHER_PORTAL;
        }

        return player.inventoryDesyncStatus = InventoryDesyncStatus.NOT_DESYNCED;
    }

    public boolean isNearNetherPortal(GrimPlayer player) {
        // Going inside nether portal with opened inventory cause desync, fixed in 1.12.2
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_1) &&
                player.pointThreeEstimator.isNearNetherPortal) {
            PacketEntitySelf playerEntity = player.compensatedEntities.self;
            // Client ignore nether portal if player has passengers or riding an entity
            return !playerEntity.inVehicle() && playerEntity.passengers.isEmpty();
        }

        return false;
    }
}
