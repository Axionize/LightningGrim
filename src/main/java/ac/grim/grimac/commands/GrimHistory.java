package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.violationdatabase.Violation;
import ac.grim.grimac.manager.violationdatabase.ViolationDatabaseManager;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@CommandAlias("grim|grimac")
public class GrimHistory extends BaseCommand {

    @Subcommand("history|hist")
    @CommandPermission("grim.history")
    @CommandAlias("gh")
    @CommandCompletion("@players")
    public void onLogs(CommandSender sender, String target, @Optional Integer page) {
        int entriesPerPage = GrimAPI.INSTANCE.getConfigManager().getConfig().getIntElse("history.entries-per-page", 15);

        String header = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("grim-history-header",
                "%prefix% &bShowing logs for &f%player% (&f%page%&b/&f%maxPages%&f)");
        String logFormat = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("grim-history-entry",
                "%prefix% &8[&f%server%&8] &bFailed &f%check% (x&c%vl%&f) &7%verbose% (&b%timeago% ago&7)");

        FoliaScheduler.getAsyncScheduler().runNow(GrimAPI.INSTANCE.getPlugin(), __ -> {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(target);
            int notNullPage = page == null ? 1 : page;

            ViolationDatabaseManager violations = GrimAPI.INSTANCE.getViolationDatabaseManager();
            int logCount = violations.getLogCount(targetPlayer.getUniqueId());
            List<Violation> logs = violations.getViolations(targetPlayer.getUniqueId(), notNullPage, entriesPerPage);

            int maxPages = (int) Math.ceil((float) logCount / entriesPerPage);
            sender.sendMessage(MessageUtil.format(header
                    .replace("%player%", targetPlayer.getName())
                    .replace("%page%", String.valueOf(notNullPage))
                    .replace("%maxPages%", String.valueOf(maxPages)))
            );

            for (int i = logs.size() - 1; i >= 0; i--) {
                Violation log = logs.get(i);
                sender.sendMessage(MessageUtil.format(logFormat
                        .replace("%player%", targetPlayer.getName())
                        .replace("%check%", log.getCheckName())
                        .replace("%verbose%", log.getVerbose())
                        .replace("%vl%", String.valueOf(log.getVl()))
                        .replace("%timeago%", getTimeAgo(log.getCreatedAt()))
                        .replace("%server%", log.getServerName())
                ));
            }
        });
    }

    protected String getTimeAgo(Date date) {
        long durationMillis = new Date().getTime() - date.getTime();

        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        durationMillis -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        durationMillis -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        durationMillis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);

        StringBuilder result = new StringBuilder();

        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0) result.append(seconds).append("s");

        return result.toString().trim();
    }

}