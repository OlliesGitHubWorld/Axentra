package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnbanIpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public UnbanIpCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.unbanip")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("unbanip-usage"));
            return true;
        }

        String input = args[0];

        if (IpBanTarget.looksLikeIp(input)) {
            if (!plugin.getDatabaseManager().isIpBanned(input)) {
                sender.sendMessage(getMessage("unbanip-not-banned").replace("%ip%", input));
                return true;
            }
            plugin.getDatabaseManager().unbanIp(input);
            sender.sendMessage(getMessage("unbanip-success").replace("%ip%", input));
            return true;
        }

        // Player name: lift the account ban and every banned IP the player has used
        String playerName = input;
        String uuid = Bukkit.getOfflinePlayer(input).getUniqueId().toString();
        boolean accountBanned = plugin.getDatabaseManager().isBannedByName(playerName);
        List<String> bannedIps = new ArrayList<>();
        for (DatabaseManager.IpHistoryEntry entry : plugin.getDatabaseManager().getIpHistory(uuid)) {
            if (!bannedIps.contains(entry.ip) && plugin.getDatabaseManager().isIpBanned(entry.ip)) {
                bannedIps.add(entry.ip);
            }
        }

        if (!accountBanned && bannedIps.isEmpty()) {
            sender.sendMessage(getMessage("unbanip-not-banned").replace("%ip%", playerName));
            return true;
        }

        boolean showIp = sender.hasPermission("Axentra.alts.ip");
        for (String ip : bannedIps) {
            plugin.getDatabaseManager().unbanIp(ip);
            sender.sendMessage(getMessage("unbanip-success").replace("%ip%",
                    showIp ? ip : getMessage("ip-hidden").replace("%player%", playerName)));
        }
        if (accountBanned) {
            plugin.getDatabaseManager().unbanPlayer(playerName);
            sender.sendMessage(getMessage("unbanip-account").replace("%player%", playerName));
        }
        return true;
    }
}
