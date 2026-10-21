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
import java.util.Arrays;
import java.util.List;

public class NickCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public NickCommand(App plugin) {
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
            completions.add("off");
        } else if (args.length == 2 && sender.hasPermission("Axentra.nick.others")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.nick")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("nick-usage"));
            return true;
        }

        Player target;
        String nickname = args[0];

        if (args.length >= 2 && sender.hasPermission("Axentra.nick.others")) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[1]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            target = (Player) sender;
        }

        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
            target.setDisplayName(target.getName());
            target.setPlayerListName(target.getName());
            plugin.getDatabaseManager().removeNickname(target.getUniqueId().toString());
            target.sendMessage(getMessage("nick-reset"));
            if (!target.equals(sender)) {
                sender.sendMessage(getMessage("nick-reset-other").replace("%player%", target.getName()));
            }
            return true;
        }

        // Check if the nickname uses color codes and player has permission
        if (nickname.matches(".*&[0-9a-fk-orA-FK-OR].*") && !sender.hasPermission("Axentra.nick.color")) {
            sender.sendMessage(getMessage("nick-no-color"));
            return true;
        }

        String coloredNick = ChatColor.translateAlternateColorCodes('&', nickname);
        String strippedNick = ChatColor.stripColor(coloredNick);

        // Validate length
        if (strippedNick.length() < 2 || strippedNick.length() > 16) {
            sender.sendMessage(getMessage("nick-invalid-length"));
            return true;
        }

        target.setDisplayName(coloredNick + ChatColor.RESET);
        target.setPlayerListName(coloredNick);
        plugin.getDatabaseManager().setNickname(target.getUniqueId().toString(), nickname);

        target.sendMessage(getMessage("nick-set").replace("%nick%", coloredNick));
        if (!target.equals(sender)) {
            sender.sendMessage(getMessage("nick-set-other")
                    .replace("%player%", target.getName())
                    .replace("%nick%", coloredNick));
        }

        return true;
    }
}
