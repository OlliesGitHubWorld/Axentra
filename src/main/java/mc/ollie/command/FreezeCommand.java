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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FreezeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public FreezeCommand(App plugin) {
        this.plugin = plugin;
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
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.freeze")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("freeze-usage"));
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

        String uuid = target.getUniqueId().toString();
        boolean isUnfreeze = label.equalsIgnoreCase("unfreeze");

        if (isUnfreeze || plugin.getDatabaseManager().isPlayerFrozen(uuid)) {
            // Unfreeze
            plugin.getDatabaseManager().unfreezePlayer(uuid);
            sender.sendMessage(getMessage("freeze-unfrozen-staff").replace("%player%", target.getName()));
            target.sendMessage(getMessage("freeze-unfrozen"));
            plugin.getDatabaseManager().logAction(
                    sender.getName(),
                    sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE",
                    "UNFREEZE", target.getName(), "");
        } else {
            // Freeze
            plugin.getDatabaseManager().freezePlayer(uuid, sender.getName());
            sender.sendMessage(getMessage("freeze-frozen-staff").replace("%player%", target.getName()));
            target.sendMessage(getMessage("freeze-frozen"));
            target.sendTitle(
                    ChatColor.RED + "" + ChatColor.BOLD + "FROZEN",
                    ChatColor.GRAY + "Do not log out!",
                    0, 100, 20
            );
            plugin.getDatabaseManager().logAction(
                    sender.getName(),
                    sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE",
                    "FREEZE", target.getName(), "");
        }
        return true;
    }
}
