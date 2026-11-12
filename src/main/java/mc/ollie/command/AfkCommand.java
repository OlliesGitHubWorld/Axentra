package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /afk [reason] (v1.9.0) — toggle AFK.
 */
public class AfkCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public AfkCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.afk")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
            return true;
        }
        String reason = args.length > 0 ? String.join(" ", args) : null;
        if (reason != null && reason.length() > 64) reason = reason.substring(0, 64);
        plugin.getAfkManager().toggle(player, reason);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
