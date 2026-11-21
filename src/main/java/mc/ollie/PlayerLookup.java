package mc.ollie;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds command targets that may have just logged out, and cleans up tab completion for every
 * Axentra command: only entries matching what was typed (prefix first, then "contains"), no vanished
 * players the sender can't see, and players who left recently for the punishment commands.
 */
public class PlayerLookup implements Listener {

    /** How long a player who logged out is still suggested for punishment commands. */
    private static final long RECENT_MS = 10 * 60 * 1000L;

    /** Commands whose first argument may be a player who already logged out. */
    private static final Set<String> OFFLINE_TARGET_COMMANDS = Set.of(
            "ban", "tempban", "banip", "tempbanip", "mute", "tempmute", "warn", "punish", "note");

    private static final Map<String, Recent> recentQuits = new ConcurrentHashMap<>();

    private record Recent(String name, UUID uuid, String ip, long quitAt) {}

    /** A resolved command target. {@code online} is null when the player is not on the server. */
    public record Target(String name, String uuid, Player online) {}

    private final App plugin;

    public PlayerLookup(App plugin) {
        this.plugin = plugin;
    }

    /**
     * Resolves a typed name: online exact name, a player who left recently, any player the database
     * knows, and finally an online name starting with the input. Returns null if nobody matches.
     */
    public static Target resolve(App plugin, String input) {
        Player exact = Bukkit.getPlayerExact(input);
        if (exact != null) return new Target(exact.getName(), exact.getUniqueId().toString(), exact);

        Recent recent = recentQuits.get(input.toLowerCase(Locale.ROOT));
        if (recent != null) return new Target(recent.name(), recent.uuid().toString(), null);

        String uuid = plugin.getDatabaseManager().findUuidByName(input);
        if (uuid != null) {
            try {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                String name = offline.getName() != null ? offline.getName() : input;
                return new Target(name, uuid, offline.getPlayer());
            } catch (IllegalArgumentException ignored) {
                // Malformed UUID in old data — fall through to the prefix match
            }
        }

        Player partial = Bukkit.getPlayer(input);
        if (partial != null) return new Target(partial.getName(), partial.getUniqueId().toString(), partial);
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        recentQuits.values().removeIf(r -> now - r.quitAt() > RECENT_MS);
        InetSocketAddress address = player.getAddress();
        String ip = address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : null;
        recentQuits.put(player.getName().toLowerCase(Locale.ROOT),
                new Recent(player.getName(), player.getUniqueId(), ip, now));
    }

    /** The IP a player who left in the last few minutes connected with, or null. */
    public static String recentIp(String uuid) {
        for (Recent recent : recentQuits.values()) {
            if (recent.uuid().toString().equals(uuid)) return recent.ip();
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (buffer.startsWith("/")) buffer = buffer.substring(1);
        int firstSpace = buffer.indexOf(' ');
        if (firstSpace < 0) return;

        String label = buffer.substring(0, firstSpace).toLowerCase(Locale.ROOT);
        if (label.startsWith("axentra:")) label = label.substring("axentra:".length());
        PluginCommand command = plugin.getServer().getPluginCommand(label);
        if (command == null || command.getPlugin() != plugin) return;

        String argsPart = buffer.substring(firstSpace + 1);
        int lastSpace = argsPart.lastIndexOf(' ');
        String token = argsPart.substring(lastSpace + 1).toLowerCase(Locale.ROOT);
        boolean firstArg = lastSpace < 0;

        CommandSender sender = event.getSender();
        List<String> source = new ArrayList<>(event.getCompletions());
        if (firstArg && OFFLINE_TARGET_COMMANDS.contains(command.getName().toLowerCase(Locale.ROOT))) {
            long now = System.currentTimeMillis();
            for (Recent recent : recentQuits.values()) {
                if (now - recent.quitAt() <= RECENT_MS && Bukkit.getPlayer(recent.uuid()) == null
                        && !source.contains(recent.name())) {
                    source.add(recent.name());
                }
            }
        }

        List<String> starts = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String completion : source) {
            if (sender instanceof Player viewer) {
                Player named = Bukkit.getPlayerExact(completion);
                if (named != null && !viewer.canSee(named)) continue;
            }
            String lower = completion.toLowerCase(Locale.ROOT);
            if (lower.startsWith(token)) starts.add(completion);
            else if (lower.contains(token)) contains.add(completion);
        }
        starts.addAll(contains);
        event.setCompletions(starts);
    }
}
