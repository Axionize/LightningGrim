package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.events.GrimQuitEvent;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            // Do this after send to avoid sending packets before the PLAY state
            event.getTasksAfterSend().add(() -> GrimAPI.INSTANCE.getPlayerDataManager().addUser(event.getUser()));
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        // Player connected too soon, perhaps late bind is off
        // Don't kick everyone on reload
        if (event.getUser().getConnectionState() == ConnectionState.PLAY && !GrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.contains(event.getUser())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("debug-pipeline-on-join", false)) {
            LogUtil.info("Pipeline: " + ChannelHelper.pipelineHandlerNamesAsString(event.getUser().getChannel()));
        }
        if (player.hasPermission("grim.alerts") && player.hasPermission("grim.alerts.enable-on-join")) {
            GrimAPI.INSTANCE.getAlertManager().toggleAlerts(player);
        }
        if (player.hasPermission("grim.verbose") && player.hasPermission("grim.verbose.enable-on-join")) {
            GrimAPI.INSTANCE.getAlertManager().toggleVerbose(player);
        }
        if (player.hasPermission("grim.brand") && player.hasPermission("grim.brand.enable-on-join")) {
            GrimAPI.INSTANCE.getAlertManager().toggleBrands(player);
        }
        if (player.hasPermission("grim.spectate") && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("spectators.hide-regardless", false)) {
            GrimAPI.INSTANCE.getSpectateManager().onLogin(player);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().remove(event.getUser());
        if (grimPlayer != null) Bukkit.getPluginManager().callEvent(new GrimQuitEvent(grimPlayer));
        GrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.remove(event.getUser());
        //Check if calling async is safe
        if (event.getUser().getProfile().getUUID() == null) return; // folia doesn't like null getPlayer()
        if (grimPlayer != null) {
            GrimAPI.INSTANCE.getAlertManager().handlePlayerQuit(grimPlayer);
            GrimAPI.INSTANCE.getSpectateManager().onQuit(grimPlayer);
        }
    }
}
