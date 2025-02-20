package ac.grim.bukkit.utils.scheduler.bukkit;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.utils.scheduler.AsyncScheduler;
import ac.grim.grimac.utils.scheduler.PlatformScheduler;
import ac.grim.grimac.utils.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class BukkitAsyncScheduler implements AsyncScheduler {

    private final BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    @Override
    public TaskHandle runNow(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskAsynchronously(GrimACBukkitLoaderPlugin.PLUGIN, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLaterAsynchronously(GrimACBukkitLoaderPlugin.PLUGIN,
                task,
                PlatformScheduler.convertTimeToTicks(delay, timeUnit))
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        return new BukkitTaskHandle(
                bukkitScheduler.runTaskTimerAsynchronously(
                    GrimACBukkitLoaderPlugin.PLUGIN,
                    task,
                    PlatformScheduler.convertTimeToTicks(delay, timeUnit),
                    PlatformScheduler.convertTimeToTicks(period, timeUnit)
            )
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(
                bukkitScheduler.runTaskTimerAsynchronously(GrimACBukkitLoaderPlugin.PLUGIN, task, initialDelayTicks, periodTicks)
        );
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        bukkitScheduler.cancelTasks(GrimACBukkitLoaderPlugin.PLUGIN);
    }
}
