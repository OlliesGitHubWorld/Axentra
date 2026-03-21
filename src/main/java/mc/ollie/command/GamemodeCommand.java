package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GamemodeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public GamemodeCommand(App plugin) {
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

    private GameMode parseGameMode(String input, String label) {
        switch (label.toLowerCase()) {
            case "gms": return GameMode.SURVIVAL;
            case "gmc": return GameMode.CREATIVE;
            case "gma": return GameMode.ADVENTURE;
            case "gmsp": return GameMode.SPECTATOR;
        }
        switch (input.toLowerCase()) {
            case "survival": case "s": case "0": return GameMode.SURVIVAL;
            case "creative": case "c": case "1": return GameMode.CREATIVE;
            case "adventure": case "a": case "2": return GameMode.ADVENTURE;
            case "spectator": case "sp": case "3": return GameMode.SPECTATOR;
            default: return null;
        }
    }

    private boolean isShortcut(String label) {
        switch (label.toLowerCase()) {
            case "gms": case "gmc": case "gma": case "gmsp": return true;
            default: return false;
        }
    }

    private String formatGameMode(GameMode gameMode) {
        switch (gameMode) {
            case SURVIVAL:  return "Survival";
            case CREATIVE:  return "Creative";
            case ADVENTURE: return "Adventure";
            case SPECTATOR: return "Spectator";
            default:        return gameMode.name();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (isShortcut(label)) {
            if (args.length == 1 && sender.hasPermission("Axentra.gamemode.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
        } else {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("survival", "creative", "adventure", "spectator"));
            } else if (args.length == 2 && sender.hasPermission("Axentra.gamemode.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.gamemode")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        boolean shortcut = isShortcut(label);
        GameMode gameMode;
        String targetArg;

        if (shortcut) {
            gameMode = parseGameMode("", label);
            targetArg = args.length >= 1 ? args[0] : null;
        } else {
            if (args.length == 0) {
                sender.sendMessage(getMessage("gamemode-usage"));
                return true;
            }
            gameMode = parseGameMode(args[0], label);
            if (gameMode == null) {
                sender.sendMessage(getMessage("gamemode-invalid").replace("%mode%", args[0]));
                return true;
            }
            targetArg = args.length >= 2 ? args[1] : null;
        }

        if (targetArg == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            player.setGameMode(gameMode);
            player.sendMessage(getMessage("gamemode-self").replace("%gamemode%", formatGameMode(gameMode)));
            return true;
        }

        if (!sender.hasPermission("Axentra.gamemode.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", targetArg));
            return true;
        }

        target.setGameMode(gameMode);
        target.sendMessage(getMessage("gamemode-received")
                .replace("%gamemode%", formatGameMode(gameMode))
                .replace("%player%", sender instanceof Player ? sender.getName() : "Console"));
        sender.sendMessage(getMessage("gamemode-other")
                .replace("%gamemode%", formatGameMode(gameMode))
                .replace("%player%", target.getName()));
        return true;
    }
}