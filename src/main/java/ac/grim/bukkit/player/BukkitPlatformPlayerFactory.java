package ac.grim.bukkit.player;

import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlatformPlayerFactory implements PlatformPlayerFactory {
    @Override
    public PlatformPlayer getFromUUID(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return null;
        } else {
            return new BukkitPlatformPlayer(player);
        }
    }

    @Override
    public PlatformPlayer getFromNativePlayerType(Object playerObject) {
        if (playerObject instanceof Player) {
            return new BukkitPlatformPlayer((Player) playerObject);
        } else {
            throw new IllegalStateException("playerObject was not of type " + Player.class.getPackage() + "." + Player.class.getName());
        }
    }
}
