package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    private final App plugin;

    public JoinLeaveListener(App plugin) {
        this.plugin = plugin;
    }

    private static final String DEFAULT_JOIN = "%player% joined the game";

    private World resolveSpawnWorld() {
        String name = plugin.getFileManager().getSettings().getString("spawn-world", "");
        if (name != null && !name.isEmpty()) {
            World world = Bukkit.getWorld(name);
            if (world != null) return world;
            plugin.getLogger().warning("spawn-world '" + name + "' not found, falling back to default world.");
        }
        return Bukkit.getWorlds().get(0);
    }
    private static final String DEFAULT_QUIT = "%player% left the game";

    private String getMessage(String path, Player player, String defaultMessage, int playerCount) {
        String message = plugin.getFileManager().getMessages().getString(path);

        if (message == null || message.equalsIgnoreCase("none")) {
            return ChatColor.YELLOW + defaultMessage.replace("%player%", player.getName());
        }

        if (message.isEmpty()) {
            return null;
        }

        message = message.replace("%player%", player.getName());
        message = message.replace("%playercount%", String.valueOf(playerCount));
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int count = Bukkit.getOnlinePlayers().size(); // includes the joining player

        if (!player.hasPlayedBefore()) {
            event.setJoinMessage(getMessage("first-join", player, DEFAULT_JOIN, count));
        } else {
            event.setJoinMessage(getMessage("join", player, DEFAULT_JOIN, count));
        }

        String joinLocation = plugin.getFileManager().getSettings().getString("join-location", "last-location");
        if (joinLocation.equalsIgnoreCase("spawn")) {
            player.teleport(resolveSpawnWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        int count = Bukkit.getOnlinePlayers().size() - 1; // exclude the leaving player
        event.setQuitMessage(getMessage("quit", event.getPlayer(), DEFAULT_QUIT, count));
    }
}