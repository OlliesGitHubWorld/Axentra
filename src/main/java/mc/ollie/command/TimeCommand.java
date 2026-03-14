package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TimeCommand implements CommandExecutor, TabCompleter {

    private static final long TIME_DAY      = 1000L;
    private static final long TIME_NOON     = 6000L;
    private static final long TIME_NIGHT    = 13000L;
    private static final long TIME_MIDNIGHT = 18000L;

    private final App plugin;

    public TimeCommand(App plugin) {
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

    private Long parseTime(String input) {
        switch (input.toLowerCase()) {
            case "day":      return TIME_DAY;
            case "noon":     return TIME_NOON;
            case "night":    return TIME_NIGHT;
            case "midnight": return TIME_MIDNIGHT;
            default:
                try { return Long.parseLong(input); }
                catch (NumberFormatException e) { return null; }
        }
    }

    private void setTimeInWorlds(CommandSender sender, long time) {
        if (sender instanceof Player) {
            ((Player) sender).getWorld().setTime(time);
        } else {
            for (World world : plugin.getServer().getWorlds()) {
                world.setTime(time);
            }
        }
    }

    private void addTimeInWorlds(CommandSender sender, long ticks) {
        if (sender instanceof Player) {
            World world = ((Player) sender).getWorld();
            world.setTime(world.getTime() + ticks);
        } else {
            for (World world : plugin.getServer().getWorlds()) {
                world.setTime(world.getTime() + ticks);
            }
        }
    }

    // Called by alias commands (/day, /night, /noon, /midnight)
    public void setNamedTime(CommandSender sender, String timeName) {
        if (!sender.hasPermission("Axentra.time")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }
        Long time = parseTime(timeName);
        if (time == null) return;
        setTimeInWorlds(sender, time);
        sender.sendMessage(getMessage("time-set").replace("%time%", timeName));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle alias commands: /day /night /noon /midnight
        String lowerLabel = label.toLowerCase();
        if (lowerLabel.equals("day") || lowerLabel.equals("night")
                || lowerLabel.equals("noon") || lowerLabel.equals("midnight")) {
            setNamedTime(sender, lowerLabel);
            return true;
        }

        if (!sender.hasPermission("Axentra.time")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("time-usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String timeArg = args[1].toLowerCase();
        Long timeValue = parseTime(timeArg);

        if (timeValue == null) {
            sender.sendMessage(getMessage("time-invalid").replace("%value%", timeArg));
            return true;
        }

        switch (subCommand) {
            case "set":
                setTimeInWorlds(sender, timeValue);
                sender.sendMessage(getMessage("time-set").replace("%time%", timeArg));
                break;
            case "add":
                addTimeInWorlds(sender, timeValue);
                sender.sendMessage(getMessage("time-add").replace("%time%", timeArg));
                break;
            default:
                sender.sendMessage(getMessage("time-usage"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.time")) return Collections.emptyList();

        String lowerLabel = label.toLowerCase();
        if (lowerLabel.equals("day") || lowerLabel.equals("night")
                || lowerLabel.equals("noon") || lowerLabel.equals("midnight")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("set", "add");
        }
        if (args.length == 2) {
            return Arrays.asList("day", "noon", "night", "midnight");
        }
        return Collections.emptyList();
    }
}