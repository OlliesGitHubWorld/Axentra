package mc.ollie;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Groups online players for /list and the Discord /online command. A player belongs to the first
 * group in config.yml list.groups whose permission Axentra.list.group.&lt;name&gt; they have;
 * everyone else lands in list.default-group. Works with any permission plugin.
 */
public final class PlayerGroups {

    public static final String PERMISSION_PREFIX = "Axentra.list.group.";

    private PlayerGroups() { }

    /** Display name -> players, in config order. Main thread only (reads permissions). */
    public static LinkedHashMap<String, List<Player>> group(App plugin, boolean includeVanished) {
        LinkedHashMap<String, List<Player>> groups = new LinkedHashMap<>();
        ConfigurationSection section = plugin.getFileManager().getSettings().getConfigurationSection("list.groups");
        List<String> keys = section != null ? new ArrayList<>(section.getKeys(false)) : List.of();
        for (String key : keys) groups.put(section.getString(key, key), new ArrayList<>());
        String fallback = plugin.getFileManager().getSettings().getString("list.default-group", "&7Players");
        groups.putIfAbsent(fallback, new ArrayList<>());

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        for (Player player : online) {
            if (!includeVanished && plugin.isVanished(player)) continue;
            String target = fallback;
            for (String key : keys) {
                if (player.hasPermission(PERMISSION_PREFIX + key)) {
                    target = section.getString(key, key);
                    break;
                }
            }
            groups.get(target).add(player);
        }
        groups.values().removeIf(List::isEmpty);
        return groups;
    }
}
