package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.FreezeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FreezeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final FreezeManager freezeManager;

    public FreezeCommand(App plugin, FreezeManager freezeManager) {
        this.plugin = plugin;
        this.freezeManager = freezeManager;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && label.equalsIgnoreCase("tempfreeze")) {
            return Arrays.asList("30s", "5m", "10m", "30m", "1h").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.freeze")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        boolean isTemp = label.equalsIgnoreCase("tempfreeze");
        if (args.length == 0 || (isTemp && args.length < 2)) {
            sender.sendMessage(getMessage(isTemp ? "tempfreeze-usage" : "freeze-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (target.hasPermission("Axentra.freeze.exempt")) {
            sender.sendMessage(getMessage("freeze-exempt").replace("%player%", target.getName()));
            return true;
        }

        if (isTemp) {
            long duration = FreezeManager.parseDuration(args[1]);
            if (duration <= 0) {
                sender.sendMessage(getMessage("tempfreeze-invalid-duration"));
                return true;
            }
            freezeManager.freeze(target, sender, duration);
            sender.sendMessage(getMessage("freeze-tempfrozen-staff")
                    .replace("%player%", target.getName())
                    .replace("%duration%", FreezeManager.formatDuration(duration)));
            return true;
        }

        boolean isUnfreeze = label.equalsIgnoreCase("unfreeze");

        if (isUnfreeze || freezeManager.isFrozen(target.getUniqueId())) {
            freezeManager.unfreeze(target.getUniqueId(), target.getName(), sender);
            sender.sendMessage(getMessage("freeze-unfrozen-staff").replace("%player%", target.getName()));
        } else {
            freezeManager.freeze(target, sender, 0L);
            sender.sendMessage(getMessage("freeze-frozen-staff").replace("%player%", target.getName()));
        }
        return true;
    }
}
