package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StaffstatsCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public StaffstatsCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.staffstats")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        // /staffstats top
        if (args.length == 1 && args[0].equalsIgnoreCase("top")) {
            showLeaderboard(sender);
            return true;
        }

        // /staffstats [player]
        String targetName;
        String targetUuid;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("staffstats-usage"));
                return true;
            }
            Player player = (Player) sender;
            targetName = player.getName();
            targetUuid = player.getUniqueId().toString();
        } else {
            targetName = args[0];
            Player targetPlayer = Bukkit.getPlayer(targetName);
            targetUuid = targetPlayer != null
                    ? targetPlayer.getUniqueId().toString()
                    : plugin.getDatabaseManager().getUuidByName(targetName);
            if (targetUuid == null) targetUuid = targetName;
        }

        showStats(sender, targetName, targetUuid);
        return true;
    }

    private void showStats(CommandSender sender, String staffName, String staffUuid) {
        int totalCases   = plugin.getDatabaseManager().getStaffCaseCount(staffName);
        int bans         = plugin.getDatabaseManager().getStaffCaseCountByType(staffName, "BAN");
        int tempBans     = plugin.getDatabaseManager().getStaffCaseCountByType(staffName, "TEMPBAN");
        int kicks        = plugin.getDatabaseManager().getStaffCaseCountByType(staffName, "KICK");
        int mutes        = plugin.getDatabaseManager().getStaffCaseCountByType(staffName, "MUTE");
        int warns        = plugin.getDatabaseManager().getStaffCaseCountByType(staffName, "WARN");
        int reportsClosed = plugin.getDatabaseManager().getStaffReportsClosed(staffName);

        long dutyDay   = staffUuid != null ? plugin.getDatabaseManager().getStaffDutyMinutes(staffUuid, "day")   : 0L;
        long dutyWeek  = staffUuid != null ? plugin.getDatabaseManager().getStaffDutyMinutes(staffUuid, "week")  : 0L;
        long dutyTotal = staffUuid != null ? plugin.getDatabaseManager().getStaffDutyMinutes(staffUuid, "total") : 0L;

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  Staff Stats: " + ChatColor.WHITE + staffName);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Total Cases:     " + ChatColor.WHITE + totalCases);
        sender.sendMessage(ChatColor.GRAY + "  Bans:            " + ChatColor.WHITE + bans
                + ChatColor.DARK_GRAY + " (+" + tempBans + " temp)");
        sender.sendMessage(ChatColor.GRAY + "  Kicks:           " + ChatColor.WHITE + kicks);
        sender.sendMessage(ChatColor.GRAY + "  Mutes:           " + ChatColor.WHITE + mutes);
        sender.sendMessage(ChatColor.GRAY + "  Warns:           " + ChatColor.WHITE + warns);
        sender.sendMessage(ChatColor.GRAY + "  Reports Closed:  " + ChatColor.WHITE + reportsClosed);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Duty Time (Today):  " + ChatColor.WHITE + formatMinutes(dutyDay));
        sender.sendMessage(ChatColor.GRAY + "  Duty Time (Week):   " + ChatColor.WHITE + formatMinutes(dutyWeek));
        sender.sendMessage(ChatColor.GRAY + "  Duty Time (Total):  " + ChatColor.WHITE + formatMinutes(dutyTotal));
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
    }

    private void showLeaderboard(CommandSender sender) {
        List<DatabaseManager.LeaderboardEntry> entries = plugin.getDatabaseManager().getStaffLeaderboard(10);

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  Staff Leaderboard " + ChatColor.GRAY + "(Top 10 by cases)");
        sender.sendMessage("");

        if (entries.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  No data available yet.");
        } else {
            int rank = 1;
            for (DatabaseManager.LeaderboardEntry entry : entries) {
                String rankColor = rank == 1 ? ChatColor.GOLD + "" : rank == 2 ? ChatColor.GRAY + "" : rank == 3 ? ChatColor.RED + "" : ChatColor.WHITE + "";
                sender.sendMessage(ChatColor.DARK_GRAY + "  #" + rank + " "
                        + rankColor + entry.staffName
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + entry.caseCount + " cases");
                rank++;
            }
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
    }

    private String formatMinutes(long minutes) {
        if (minutes <= 0) return "0m";
        long hours = minutes / 60;
        long mins  = minutes % 60;
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions.add("top");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        }
        return completions;
    }
}
