package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public ReportCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        if (!sender.hasPermission("Axentra.report")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("report-usage"));
            return true;
        }

        Player reporter = (Player) sender;
        UUID reporterUuid = reporter.getUniqueId();

        // Check cooldown
        int cooldownSeconds = plugin.getFileManager().getSettings().getInt("report-cooldown-seconds", 60);
        Long lastReport = plugin.getReportCooldowns().get(reporterUuid);
        if (lastReport != null) {
            long elapsed = (System.currentTimeMillis() - lastReport) / 1000L;
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                sender.sendMessage(getMessage("report-cooldown").replace("%seconds%", String.valueOf(remaining)));
                return true;
            }
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (target.equals(reporter)) {
            sender.sendMessage(getMessage("report-self"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) reasonBuilder.append(" ");
        }
        String reason = reasonBuilder.toString();

        // Get chat snapshot of target
        String chatLog = null;
        ChatListener chatListener = plugin.getChatListener();
        if (chatListener != null) {
            chatLog = chatListener.getChatSnapshot(target.getUniqueId());
        }

        // Create the report in DB
        plugin.getDatabaseManager().createReport(
                target.getName(),
                target.getUniqueId().toString(),
                reason,
                reporter.getName(),
                reporterUuid.toString(),
                chatLog
        );

        // Update cooldown
        plugin.getReportCooldowns().put(reporterUuid, System.currentTimeMillis());

        reporter.sendMessage(getMessage("report-sent")
                .replace("%player%", target.getName())
                .replace("%reason%", reason));

        // Notify all online staff on duty with reports permission
        String notifyMsg = getMessage("report-notify")
                .replace("%reporter%", reporter.getName())
                .replace("%player%", target.getName())
                .replace("%reason%", reason);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("Axentra.reports")
                    && plugin.getDutyManager().isOnDuty(online.getUniqueId())) {
                online.sendMessage(notifyMsg);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
