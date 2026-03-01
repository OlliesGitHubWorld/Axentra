package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BanCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public BanCommand(App plugin) {
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
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.ban")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("ban-usage"));
            return true;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        // Build reason
        String reason;
        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]);
                if (i < args.length - 1) reasonBuilder.append(" ");
            }
            reason = reasonBuilder.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("ban-default-reason", "You have been banned!");
        }

        String bannedBy = sender.getName();
        String uuid = target != null ? target.getUniqueId().toString() : playerName;

        // Check if already banned
        if (plugin.getDatabaseManager().isBannedByName(playerName)) {
            sender.sendMessage(getMessage("ban-already-banned").replace("%player%", playerName));
            return true;
        }
        // Ban the player
        plugin.getDatabaseManager().banPlayer(playerName, uuid, reason, bannedBy, null);

        if (target != null) {
            String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
            String rawBanMessage = plugin.getFileManager().getMessages().getString("ban-message");

            rawBanMessage = rawBanMessage.replace("%reason%", reason);
            rawBanMessage = rawBanMessage.replace("%banned-by%", bannedBy);

            if (appealLink.isEmpty()) {
                rawBanMessage = rawBanMessage.replace("\n&7Appeal: &f%appeal%", "");
            } else {
                rawBanMessage = rawBanMessage.replace("%appeal%", appealLink);
            }

            target.kickPlayer(ChatColor.translateAlternateColorCodes('&', rawBanMessage));
        }

        sender.sendMessage(getMessage("ban-success")
                .replace("%player%", playerName)
                .replace("%reason%", reason));

        // Announce if enabled
        if (plugin.getFileManager().getSettings().getBoolean("announce-ban")) {
            plugin.getServer().broadcastMessage(getMessage("ban-announce")
                    .replace("%player%", playerName)
                    .replace("%reason%", reason));
        }

        return true;
    }
}