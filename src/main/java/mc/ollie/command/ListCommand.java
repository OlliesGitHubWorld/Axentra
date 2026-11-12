package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.PlayerGroups;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * /list (v1.9.0) — online players grouped by Axentra.list.group.&lt;name&gt; (order and names in
 * config.yml), with AFK and on-duty tags. Vanished players are only shown with Axentra.vanish.see,
 * and the count matches what the viewer can see.
 */
public class ListCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public ListCommand(App plugin) {
        this.plugin = plugin;
    }

    private String msg(String key) {
        return plugin.getMessageManager().getMessage(key);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.list")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }
        boolean seeVanished = !(sender instanceof Player) || sender.hasPermission("Axentra.vanish.see");
        LinkedHashMap<String, List<Player>> groups = PlayerGroups.group(plugin, seeVanished);
        int count = groups.values().stream().mapToInt(List::size).sum();

        sender.sendMessage(msg("list-header").replace("%online%", String.valueOf(count))
                .replace("%max%", String.valueOf(Bukkit.getMaxPlayers())));
        String afkTag = msg("list-tag-afk");
        String dutyTag = msg("list-tag-duty");
        String vanishTag = msg("list-tag-vanished");
        for (var group : groups.entrySet()) {
            StringBuilder names = new StringBuilder();
            for (Player p : group.getValue()) {
                if (names.length() > 0) names.append(ChatColor.GRAY).append(", ");
                names.append(ChatColor.WHITE).append(p.getName());
                if (plugin.getAfkManager() != null && plugin.getAfkManager().isAfk(p)) names.append(afkTag);
                if (plugin.getDutyManager() != null && plugin.getDutyManager().isOnDuty(p.getUniqueId())) names.append(dutyTag);
                if (plugin.isVanished(p)) names.append(vanishTag);
            }
            sender.sendMessage(msg("list-group").replace("%group%", ChatColor.translateAlternateColorCodes('&', group.getKey()))
                    .replace("%count%", String.valueOf(group.getValue().size())).replace("%players%", names.toString()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
