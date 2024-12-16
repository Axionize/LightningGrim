package ac.grim.grimac.manager.violationdatabase;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ViolationDatabaseManager implements Initable {

    private final Plugin plugin;

    private ViolationDatabase database;

    public ViolationDatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        ConfigManager config = GrimAPI.INSTANCE.getConfigManager().getConfig();
        String dbType = config.getStringElse("history.database.type", "SQLITE").toUpperCase();

        if (dbType.equals("SQLITE")) {
            this.database = new SQLiteViolationDatabase(plugin);
        } else if (dbType.equals("MYSQL")) {
            String url = config.getStringElse("history.database.host", "jdbc:mysql://localhost:3306/");
            String database = config.getStringElse("history.database.database", "grimac");
            String username = config.getStringElse("history.database.username", "root");
            String password = config.getStringElse("history.database.password", "password");

            this.database = new MySQLViolationDatabase(plugin, url, database, username, password);
        } else {
            this.database = new SQLiteViolationDatabase(plugin);
            plugin.getLogger().log(Level.SEVERE, "Invalid database type: " + dbType + ". Defaulting to SQLite");
        }

        this.database.connect();
    }

    public void logAlert(GrimPlayer player, String verbose, String checkName, int vls) {
        FoliaScheduler.getAsyncScheduler().runNow(plugin, __ -> database.logAlert(player, verbose, checkName, vls));
    }

    public int getLogCount(UUID player) {
        return database.getLogCount(player);
    }

    public List<Violation> getViolations(UUID player, int page, int limit) {
        return database.getViolations(player, page, limit);
    }

}