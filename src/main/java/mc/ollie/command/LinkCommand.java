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
 * /link and /unlink (v1.9.0) — link a Minecraft account to Discord.
 * Registered only when the Discord module is on and JDA loaded.
 */
public class LinkCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public LinkCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.link")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
            return true;
        }
        if (plugin.getLinkService() == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("discord-module-off"));
            return true;
        }
        if (command.getName().equalsIgnoreCase("unlink")) {
            plugin.getLinkService().handleUnlinkCommand(player);
        } else {
            plugin.getLinkService().handleLinkCommand(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
