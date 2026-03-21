package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WarnsCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public WarnsCommand(App plugin) {
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
            List<String> completions = new ArrayList<>(plugin.getDatabaseManager().getActiveWarnedNames());
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!completions.contains(player.getName())) completions.add(player.getName());
            }
            return completions;
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.warns")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        String playerName;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("warns-usage"));
                return true;
            }
            playerName = sender.getName();
        } else {
            playerName = args[0];
        }

        ResultSet warns = plugin.getDatabaseManager().getWarnsByName(playerName);
        if (warns == null) {
            sender.sendMessage(getMessage("warns-error"));
            return true;
        }

        try {
            List<String[]> entries = new ArrayList<>();
            while (warns.next()) {
                entries.add(new String[]{
                        warns.getString("reason"),
                        warns.getString("warned_by"),
                        warns.getString("warned_at")
                });
            }

            if (entries.isEmpty()) {
                sender.sendMessage(getMessage("warns-none").replace("%player%", playerName));
                return true;
            }

            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "---- Warnings for " + playerName + " (" + entries.size() + ") ----");
            for (int i = 0; i < entries.size(); i++) {
                String[] entry = entries.get(i);
                sender.sendMessage(ChatColor.GRAY + "#" + (i + 1) + " " + ChatColor.WHITE + entry[0]
                        + ChatColor.GRAY + " by " + ChatColor.YELLOW + entry[1]
                        + ChatColor.GRAY + " on " + entry[2]);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to read warns: " + e.getMessage());
            sender.sendMessage(getMessage("warns-error"));
        }

        return true;
    }
}