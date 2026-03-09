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

public class FeedCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public FeedCommand(App plugin) {
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

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("Axentra.feed.others")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.feed")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            feedPlayer(player);
            player.sendMessage(getMessage("feed-self"));
            return true;
        }

        if (!sender.hasPermission("Axentra.feed.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        feedPlayer(target);
        target.sendMessage(getMessage("feed-received").replace("%player%", sender.getName()));
        sender.sendMessage(getMessage("feed-other").replace("%player%", target.getName()));
        return true;
    }
}