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

public class FlyCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public FlyCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.fly")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            boolean newFlightState = !player.getAllowFlight();
            player.setAllowFlight(newFlightState);
            if (newFlightState) {
                player.sendMessage(getMessage("fly-enabled"));
            } else {
                player.sendMessage(getMessage("fly-disabled"));
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (!sender.hasPermission("Axentra.fly.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        boolean newFlightState = !target.getAllowFlight();
        target.setAllowFlight(newFlightState);
        if (newFlightState) {
            target.sendMessage(getMessage("fly-enabled"));
        } else {
            target.sendMessage(getMessage("fly-disabled"));
        }
        sender.sendMessage(getMessage("fly-other").replace("%player%", target.getName()));
        return true;
    }
}
