package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.command.StaffchatCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {

    private final App plugin;
    private final StaffchatCommand staffchatCommand;

    public ChatListener(App plugin, StaffchatCommand staffchatCommand) {
        this.plugin = plugin;
        this.staffchatCommand = staffchatCommand;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Muted player check
        if (plugin.getDatabaseManager().isMuted(uuid.toString())) {
            event.setCancelled(true);
            player.sendMessage(getMessage("mute-blocked"));
            return;
        }

        // Staffchat toggle - redirect to staff chat
        if (plugin.isStaffChatToggled(uuid)) {
            event.setCancelled(true);
            staffchatCommand.sendStaffMessage(player.getName(), event.getMessage());
            return;
        }

        // Global mutechat check
        if (plugin.isChatMuted() && !player.hasPermission("Axentra.mutechat.bypass")) {
            event.setCancelled(true);
            player.sendMessage(getMessage("mutechat-blocked"));
            return;
        }

        // Slowchat check
        int slowSeconds = plugin.getSlowChatSeconds();
        if (slowSeconds > 0 && !player.hasPermission("Axentra.slowchat.bypass")) {
            Long lastTime = plugin.getLastChatTime().get(uuid);
            long now = System.currentTimeMillis();
            if (lastTime != null) {
                long remaining = (slowSeconds * 1000L) - (now - lastTime);
                if (remaining > 0) {
                    event.setCancelled(true);
                    player.sendMessage(getMessage("slowchat-wait")
                            .replace("%seconds%", String.valueOf((int) Math.ceil(remaining / 1000.0))));
                    return;
                }
            }
            plugin.getLastChatTime().put(uuid, now);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDatabaseManager().isMuted(player.getUniqueId().toString())) {
            return;
        }

        List<String> blockedCommands = plugin.getFileManager().getSettings().getStringList("mute-blocked-commands");
        String commandLine = event.getMessage().substring(1).toLowerCase();
        String commandName = commandLine.split(" ")[0];

        for (String blocked : blockedCommands) {
            if (commandName.equals(blocked.toLowerCase())) {
                event.setCancelled(true);
                player.sendMessage(getMessage("mute-command-blocked"));
                return;
            }
        }
    }
}
