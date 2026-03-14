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

public class WeatherCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public WeatherCommand(App plugin) {
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

    private void setWeather(CommandSender sender, String type) {
        if (sender instanceof Player) {
            World world = ((Player) sender).getWorld();
            applyWeather(world, type);
        } else {
            for (World world : plugin.getServer().getWorlds()) {
                applyWeather(world, type);
            }
        }
    }

    private void applyWeather(World world, String type) {
        switch (type) {
            case "clear":
                world.setStorm(false);
                world.setThundering(false);
                break;
            case "rain":
                world.setStorm(true);
                world.setThundering(false);
                break;
            case "storm":
                world.setStorm(true);
                world.setThundering(true);
                break;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.weather")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        String lowerLabel = label.toLowerCase();

        // Handle alias commands: /sun /sky /rain /storm
        switch (lowerLabel) {
            case "sun":
            case "sky":
                setWeather(sender, "clear");
                sender.sendMessage(getMessage("weather-clear"));
                return true;
            case "rain":
                setWeather(sender, "rain");
                sender.sendMessage(getMessage("weather-rain"));
                return true;
            case "storm":
                setWeather(sender, "storm");
                sender.sendMessage(getMessage("weather-storm"));
                return true;
        }

        // /weather <clear|rain|storm>
        if (args.length < 1) {
            sender.sendMessage(getMessage("weather-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "clear":
            case "sun":
            case "sky":
                setWeather(sender, "clear");
                sender.sendMessage(getMessage("weather-clear"));
                break;
            case "rain":
                setWeather(sender, "rain");
                sender.sendMessage(getMessage("weather-rain"));
                break;
            case "storm":
            case "thunder":
                setWeather(sender, "storm");
                sender.sendMessage(getMessage("weather-storm"));
                break;
            default:
                sender.sendMessage(getMessage("weather-usage"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.weather")) return Collections.emptyList();

        String lowerLabel = label.toLowerCase();
        if (lowerLabel.equals("sun") || lowerLabel.equals("sky")
                || lowerLabel.equals("rain") || lowerLabel.equals("storm")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("clear", "rain", "storm");
        }
        return Collections.emptyList();
    }
}