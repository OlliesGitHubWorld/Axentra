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
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class GodCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final Set<UUID> godPlayers = new HashSet<>();

    public GodCommand(App plugin) {
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

    public boolean isGod(Player player) {
        return godPlayers.contains(player.getUniqueId());
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
        if (!sender.hasPermission("Axentra.god")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            toggleGod(player);
            if (isGod(player)) {
                player.sendMessage(getMessage("god-enabled"));
            } else {
                player.sendMessage(getMessage("god-disabled"));
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (!sender.hasPermission("Axentra.god.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        toggleGod(target);
        if (isGod(target)) {
            target.sendMessage(getMessage("god-other-enabled").replace("%player%", sender.getName()));
            sender.sendMessage(getMessage("god-enabled-other").replace("%player%", target.getName()));
        } else {
            target.sendMessage(getMessage("god-other-disabled").replace("%player%", sender.getName()));
            sender.sendMessage(getMessage("god-disabled-other").replace("%player%", target.getName()));
        }
        return true;
    }

    private void toggleGod(Player player) {
        UUID uuid = player.getUniqueId();
        if (godPlayers.contains(uuid)) {
            godPlayers.remove(uuid);
        } else {
            godPlayers.add(uuid);
        }
    }
}