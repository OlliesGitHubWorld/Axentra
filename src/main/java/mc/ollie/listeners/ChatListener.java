package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.command.StaffchatCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final App plugin;
    private final StaffchatCommand staffchatCommand;

    private static final int MAX_CHAT_HISTORY = 20;
    private final Map<UUID, LinkedList<String>> chatHistory = new ConcurrentHashMap<>();

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

    public String getChatSnapshot(UUID playerUuid) {
        LinkedList<String> history = chatHistory.get(playerUuid);
        if (history == null || history.isEmpty()) return null;
        synchronized (history) {
            return String.join("\n", history);
        }
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

        // Track chat history for case snapshots
        chatHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
        LinkedList<String> history = chatHistory.get(uuid);
        synchronized (history) {
            history.addLast(player.getName() + ": " + event.getMessage());
            while (history.size() > MAX_CHAT_HISTORY) {
                history.removeFirst();
            }
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
