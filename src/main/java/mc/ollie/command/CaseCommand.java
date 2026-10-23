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

        // Clickable history link
        if (sender instanceof Player) {
            sender.sendMessage("");
            TextComponent historyLink = new TextComponent("  [View full history]");
            historyLink.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            historyLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history " + caseInfo.playerName));
            historyLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(ChatColor.YELLOW + "View all cases for " + caseInfo.playerName)));
            ((Player) sender).spigot().sendMessage(historyLink);
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
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
