package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeListener implements Listener {

    private final App plugin;
    private final Map<UUID, BukkitTask> messageTasks = new ConcurrentHashMap<>();

    public FreezeListener(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private boolean isFrozen(UUID uuid) {
        return plugin.getDatabaseManager().isPlayerFrozen(uuid.toString());
    }

    public void startFreezeMessages(Player player) {
        UUID uuid = player.getUniqueId();
        stopFreezeMessages(uuid);

        FileConfiguration protection = plugin.getFileManager().getProtection();
        int interval = protection.getInt("freeze.message-interval", 5);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isFrozen(uuid)) {
                stopFreezeMessages(uuid);
                return;
            }
            player.sendMessage(getMessage("freeze-reminder"));
            player.sendTitle(
                    ChatColor.RED + "" + ChatColor.BOLD + "FROZEN",
                    ChatColor.GRAY + "Do not log out!",
                    0, 40, 10
            );
        }, interval * 20L, interval * 20L);

        messageTasks.put(uuid, task);
    }

    public void stopFreezeMessages(UUID uuid) {
        BukkitTask task = messageTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isFrozen(event.getPlayer().getUniqueId())) return;
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player && isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isFrozen(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && isFrozen(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!isFrozen(event.getPlayer().getUniqueId())) return;

        FileConfiguration protection = plugin.getFileManager().getProtection();
        List<String> allowed = protection.getStringList("freeze.allowed-commands");
        String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();

        for (String allowedCmd : allowed) {
            if (command.equals(allowedCmd.toLowerCase())) return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(getMessage("freeze-command-blocked"));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!isFrozen(event.getPlayer().getUniqueId())) return;

        FileConfiguration protection = plugin.getFileManager().getProtection();
        if (protection.getBoolean("freeze.chat-only-with-staff", true)) {
            event.setCancelled(true);
            String msg = ChatColor.GRAY + "[Frozen] " + event.getPlayer().getName() + ": " + ChatColor.WHITE + event.getMessage();
            event.getPlayer().sendMessage(msg);
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("Axentra.freeze")) {
                    staff.sendMessage(msg);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        stopFreezeMessages(uuid);

        if (!isFrozen(uuid)) return;

        FileConfiguration protection = plugin.getFileManager().getProtection();
        String logoutAction = protection.getString("freeze.logout-action", "ALERT");

        switch (logoutAction.toUpperCase()) {
            case "BAN":
                String reason = getMessage("freeze-logout-ban-reason");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getDatabaseManager().banPlayer(event.getPlayer().getName(), uuid.toString(), reason, "CONSOLE", null);
                    plugin.getDatabaseManager().unfreezePlayer(uuid.toString());
                    plugin.getDatabaseManager().logAction("CONSOLE", "CONSOLE", "BAN",
                            event.getPlayer().getName(), "Logged out while frozen");
                });
                // Alert staff
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("Axentra.freeze")) {
                        staff.sendMessage(getMessage("freeze-logout-banned")
                                .replace("%player%", event.getPlayer().getName()));
                    }
                }
                break;
            case "ALERT":
            default:
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("Axentra.freeze")) {
                        staff.sendMessage(getMessage("freeze-logout-alert")
                                .replace("%player%", event.getPlayer().getName()));
                    }
                }
                break;
            case "NONE":
                break;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(getMessage("freeze-still-frozen"));
                    player.sendTitle(
                            ChatColor.RED + "" + ChatColor.BOLD + "FROZEN",
                            ChatColor.GRAY + "Do not log out!",
                            0, 100, 20
                    );
                    startFreezeMessages(player);
                }
            }, 5L);
        }
    }
}
