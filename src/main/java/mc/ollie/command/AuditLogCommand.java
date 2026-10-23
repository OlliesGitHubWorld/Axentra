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
import java.util.List;

public class AuditLogCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private static final int ENTRIES_PER_PAGE = 10;

    public AuditLogCommand(App plugin) {
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
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("Axentra.admin")) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.auditlog")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("auditlog-usage"));
            return true;
        }

        String staffName = args[0];
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        List<DatabaseManager.AuditEntry> allEntries = plugin.getDatabaseManager().getAuditLogByStaff(staffName, 100);

        if (allEntries.isEmpty()) {
            sender.sendMessage(getMessage("auditlog-empty").replace("%staff%", staffName));
            return true;
        }

        int totalPages = (int) Math.ceil((double) allEntries.size() / ENTRIES_PER_PAGE);
        if (page < 1 || page > totalPages) {
            sender.sendMessage(ChatColor.RED + "Page " + page + " does not exist. (1-" + totalPages + ")");
            return true;
        }

        int start = (page - 1) * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, allEntries.size());

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  Audit Log: " + staffName
                + ChatColor.GRAY + " (Page " + page + "/" + totalPages + ")");
        sender.sendMessage("");

        for (int i = start; i < end; i++) {
            DatabaseManager.AuditEntry entry = allEntries.get(i);
            String actionColor = getActionColor(entry.action);
            sender.sendMessage(ChatColor.GRAY + "  " + formatDate(entry.timestamp) + " "
                    + ChatColor.translateAlternateColorCodes('&', actionColor + entry.action)
                    + ChatColor.GRAY + (entry.target != null ? " on " + ChatColor.WHITE + entry.target : "")
                    + (entry.details != null ? ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + truncate(entry.details, 30) : ""));
        }

        if (totalPages > 1) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GRAY + "  Use /auditlog " + staffName + " <page> to navigate.");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        return true;
    }

    private String getActionColor(String action) {
        if (action == null) return "&7";
        String upper = action.toUpperCase();
        if (upper.contains("BAN")) return "&c";
        if (upper.contains("MUTE")) return "&6";
        if (upper.contains("WARN")) return "&e";
        if (upper.contains("KICK")) return "&d";
        if (upper.contains("NOTE")) return "&b";
        return "&7";
    }

    private String formatDate(String timestamp) {
        if (timestamp == null) return "";
        if (timestamp.length() >= 16) return timestamp.substring(5, 16);
        return timestamp;
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}
