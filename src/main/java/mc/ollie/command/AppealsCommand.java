package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /appeals [open|all] (v1.9.0) — opens the appeals GUI. Appeals are submitted in Discord.
 */
public class AppealsCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public AppealsCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.appeals")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
            return true;
        }
        if (!plugin.openAppealsGui(player, args.length == 0 || !args[0].equalsIgnoreCase("all"))) {
            sender.sendMessage(plugin.getMessageManager().getMessage("discord-module-off"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("open", "all"));
            options.removeIf(o -> !o.startsWith(args[0].toLowerCase()));
            return options;
        }
        return Collections.emptyList();
    }
}
