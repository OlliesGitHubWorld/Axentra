package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.ChatColor;
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
    private static final String DEFAULT_QUIT = "%player% left the game";

    private String getMessage(String path, Player player, String defaultMessage) {
        String message = plugin.getFileManager().getMessages().getString(path);

        if (message == null || message.equalsIgnoreCase("none")) {
            return ChatColor.YELLOW + defaultMessage.replace("%player%", player.getName());
        }

        if (message.isEmpty()) {
            return null;
        }

        message = message.replace("%player%", player.getName());
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            event.setJoinMessage(getMessage("first-join", player, DEFAULT_JOIN));
        } else {
            event.setJoinMessage(getMessage("join", player, DEFAULT_JOIN));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(getMessage("quit", event.getPlayer(), DEFAULT_QUIT));
    }
}