package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaseCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public CaseCommand(App plugin) {
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
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String sub : new String[]{"resolve", "delete"}) {
                if (sub.startsWith(input)) suggestions.add(sub);
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.case")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("case-usage"));
            return true;
        }

        // /case resolve <id>
        if (args[0].equalsIgnoreCase("resolve") && args.length >= 2) {
            return handleResolve(sender, args[1]);
        }

        // /case delete <id>
        if (args[0].equalsIgnoreCase("delete") && args.length >= 2) {
            return handleDelete(sender, args[1]);
        }

        int caseId;
        try {
            caseId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("case-invalid-id"));
            return true;
        }

        DatabaseManager.CaseInfo caseInfo = plugin.getDatabaseManager().getCase(caseId);
        if (caseInfo == null) {
            sender.sendMessage(getMessage("case-not-found").replace("%id%", String.valueOf(caseId)));
            return true;
        }

        String statusColor = caseInfo.active ? "&a" : "&c";
        String statusText = caseInfo.active ? "Active" : "Resolved";

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  Case #" + caseInfo.id);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Type: " + ChatColor.WHITE + getTypeDisplay(caseInfo.type));
        sender.sendMessage(ChatColor.GRAY + "  Player: " + ChatColor.WHITE + caseInfo.playerName);
        sender.sendMessage(ChatColor.GRAY + "  Reason: " + ChatColor.WHITE + (caseInfo.reason != null ? caseInfo.reason : "No reason"));
        sender.sendMessage(ChatColor.GRAY + "  Issued by: " + ChatColor.WHITE + caseInfo.issuedBy);
        sender.sendMessage(ChatColor.GRAY + "  Date: " + ChatColor.WHITE + formatDate(caseInfo.issuedAt));
        if (caseInfo.expiresAt != null) {
            sender.sendMessage(ChatColor.GRAY + "  Expires: " + ChatColor.WHITE + formatDate(caseInfo.expiresAt));
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7Status: " + statusColor + statusText));

        if (caseInfo.chatLog != null && !caseInfo.chatLog.isEmpty()) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "  Chat Log:");
            for (String line : caseInfo.chatLog.split("\n")) {
                sender.sendMessage(ChatColor.GRAY + "    " + line);
            }
        }

        // Clickable actions
        if (sender instanceof Player) {
            sender.sendMessage("");
            TextComponent actions = new TextComponent("  ");

            TextComponent historyLink = new TextComponent("[History]");
            historyLink.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            historyLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history " + caseInfo.playerName));
            historyLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(ChatColor.YELLOW + "View all cases for " + caseInfo.playerName)));
            actions.addExtra(historyLink);

            if (caseInfo.active) {
                actions.addExtra(new TextComponent(" "));
                TextComponent resolveLink = new TextComponent("[Resolve]");
                resolveLink.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                resolveLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/case resolve " + caseInfo.id));
                resolveLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(ChatColor.YELLOW + "Mark this case as resolved")));
                actions.addExtra(resolveLink);
            }

            if (sender.hasPermission("Axentra.case.delete")) {
                actions.addExtra(new TextComponent(" "));
                TextComponent deleteLink = new TextComponent("[Delete]");
                deleteLink.setColor(net.md_5.bungee.api.ChatColor.RED);
                deleteLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/case delete " + caseInfo.id));
                deleteLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(ChatColor.RED + "Permanently delete this case")));
                actions.addExtra(deleteLink);
            }

            ((Player) sender).spigot().sendMessage(actions);
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        return true;
    }

    private boolean handleResolve(CommandSender sender, String idStr) {
        int caseId;
        try {
            caseId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("case-invalid-id"));
            return true;
        }

        DatabaseManager.CaseInfo caseInfo = plugin.getDatabaseManager().getCase(caseId);
        if (caseInfo == null) {
            sender.sendMessage(getMessage("case-not-found").replace("%id%", idStr));
            return true;
        }

        if (!caseInfo.active) {
            sender.sendMessage(getMessage("case-already-resolved").replace("%id%", idStr));
            return true;
        }

        plugin.getDatabaseManager().resolveCase(caseId);
        plugin.getDatabaseManager().logAction(
                sender.getName(),
                sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE",
                "CASE_RESOLVE", caseInfo.playerName, "Resolved case #" + caseId);
        sender.sendMessage(getMessage("case-resolved").replace("%id%", String.valueOf(caseId)));
        return true;
    }

    private boolean handleDelete(CommandSender sender, String idStr) {
        if (!sender.hasPermission("Axentra.case.delete")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        int caseId;
        try {
            caseId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("case-invalid-id"));
            return true;
        }

        DatabaseManager.CaseInfo caseInfo = plugin.getDatabaseManager().getCase(caseId);
        if (caseInfo == null) {
            sender.sendMessage(getMessage("case-not-found").replace("%id%", idStr));
            return true;
        }

        plugin.getDatabaseManager().deleteCase(caseId);
        plugin.getDatabaseManager().logAction(
                sender.getName(),
                sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE",
                "CASE_DELETE", caseInfo.playerName, "Deleted case #" + caseId);
        sender.sendMessage(getMessage("case-deleted").replace("%id%", String.valueOf(caseId)));
        return true;
    }

    private String getTypeDisplay(String type) {
        switch (type.toUpperCase()) {
            case "BAN":      return ChatColor.RED + "Ban";
            case "TEMPBAN":  return ChatColor.RED + "Temp Ban";
            case "MUTE":     return ChatColor.GOLD + "Mute";
            case "TEMPMUTE": return ChatColor.GOLD + "Temp Mute";
            case "WARN":     return ChatColor.YELLOW + "Warning";
            case "KICK":     return ChatColor.LIGHT_PURPLE + "Kick";
            default:         return ChatColor.GRAY + type;
        }
    }

    private String formatDate(String timestamp) {
        if (timestamp == null) return "N/A";
        // Try parsing as millisecond timestamp (from setTimestamp in SQLite)
        try {
            long millis = Long.parseLong(timestamp);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.format(new java.util.Date(millis));
        } catch (NumberFormatException ignored) {}
        // SQLite timestamps come as "YYYY-MM-DD HH:MM:SS"
        if (timestamp.length() >= 16) {
            return timestamp.substring(0, 16);
        }
        return timestamp;
    }
}
