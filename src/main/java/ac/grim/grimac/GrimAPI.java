package ac.grim.grimac;

import ac.grim.grimac.manager.*;
import ac.grim.grimac.manager.config.BaseConfigManager;
import ac.grim.grimac.manager.violationdatabase.ViolationDatabaseManager;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.PlayerDataManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public enum GrimAPI {
    INSTANCE;

    private final BaseConfigManager configManager = new BaseConfigManager();
    private final AlertManagerImpl alertManager = new AlertManagerImpl();
    private final SpectateManager spectateManager = new SpectateManager();
    private final DiscordManager discordManager = new DiscordManager();
    private final PlayerDataManager playerDataManager = new PlayerDataManager();
    private final TickManager tickManager = new TickManager();
    private final GrimExternalAPI externalAPI = new GrimExternalAPI(this);
    private ViolationDatabaseManager violationDatabaseManager;
    private InitManager initManager;
    private JavaPlugin plugin;
    private boolean criticalLoadFailure = false; // Track critical failures

    public void load(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.violationDatabaseManager = new ViolationDatabaseManager(plugin);
        initManager = new InitManager();
        initManager.load(); // Load all initializers
    }

    public void start(final JavaPlugin plugin) {
        this.plugin = plugin;
        if (initManager != null) {
            initManager.start();
        }

        if (criticalLoadFailure) {
            LogUtil.error("GrimAC encountered one or more critical error(s) during initialization.");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    public void stop(final JavaPlugin plugin) {
        this.plugin = plugin;
        if (initManager != null) {
            initManager.stop();
        }
    }

    // Method to signal a critical failure
    public void signalCriticalFailure() {
        criticalLoadFailure = true;
    }
}
