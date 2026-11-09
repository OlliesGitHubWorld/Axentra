package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.FreezeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class FreezeListener implements Listener {

    private final App plugin;
    private final FreezeManager freezeManager;

    public FreezeListener(App plugin, FreezeManager freezeManager) {
        this.plugin = plugin;
        this.freezeManager = freezeManager;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private boolean isFrozen(UUID uuid) {
        return freezeManager.isFrozen(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isFrozen(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double radius = plugin.getFileManager().getProtection().getDouble("freeze.movement-radius", 1.0);
        Location anchor = freezeManager.getAnchor(player.getUniqueId());

        if (radius <= 0 || anchor == null || !anchor.getWorld().equals(to.getWorld())) {
            // Fully locked: only allow looking around inside the current block
            if (from.getBlockX() != to.getBlockX() ||
                from.getBlockY() != to.getBlockY() ||
                from.getBlockZ() != to.getBlockZ()) {
                event.setTo(withRotation(from, to));
            }
            return;
        }

        if (!withinRadius(anchor, to, radius)) {
            Location back = withinRadius(anchor, from, radius) ? from : anchor;
            event.setTo(withRotation(back, to));
        }
    }

    /** Horizontal distance from the anchor, plus a cap on how far up the player can go. */
    private boolean withinRadius(Location anchor, Location loc, double radius) {
        double dx = loc.getX() - anchor.getX();
        double dz = loc.getZ() - anchor.getZ();
        if (dx * dx + dz * dz > radius * radius) return false;
        return loc.getY() <= anchor.getY() + radius + 1.5;
    }

    private Location withRotation(Location position, Location rotation) {
        Location loc = position.clone();
        loc.setYaw(rotation.getYaw());
        loc.setPitch(rotation.getPitch());
        return loc;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!isFrozen(event.getPlayer().getUniqueId())) return;
        // Plugins/staff can still move a frozen player; re-centre the movement area on the new spot
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
            return;
        }
        Location anchor = freezeManager.getAnchor(event.getPlayer().getUniqueId());
        if (anchor != null && event.getTo() != null) {
            anchor.setWorld(event.getTo().getWorld());
            anchor.setX(event.getTo().getX());
            anchor.setY(event.getTo().getY());
            anchor.setZ(event.getTo().getZ());
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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!isFrozen(event.getPlayer().getUniqueId())) return;

        FileConfiguration protection = plugin.getFileManager().getProtection();
        if (protection.getBoolean("freeze.chat-only-with-staff", true)) {
            event.setCancelled(true);
            String format = protection.getString("freeze.chat-format", "%tag%&7%player%: &f%message%");
            String msg = ChatColor.translateAlternateColorCodes('&', format
                    .replace("%tag%", protection.getString("freeze.tag", "&b&l[Frozen] &r"))
                    .replace("%player%", event.getPlayer().getName()))
                    .replace("%message%", event.getMessage());
            event.getPlayer().sendMessage(msg);
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("Axentra.freeze") && !staff.equals(event.getPlayer())) {
                    staff.sendMessage(msg);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!isFrozen(uuid)) return;
        freezeManager.handleQuit(player);

        FileConfiguration protection = plugin.getFileManager().getProtection();
        String logoutAction = protection.getString("freeze.logout-action", "ALERT");

        switch (logoutAction.toUpperCase()) {
            case "BAN":
                String reason = protection.getString("freeze.logout-ban-reason", getMessage("freeze-logout-ban-reason"));
                long duration = FreezeManager.parseDuration(protection.getString("freeze.logout-ban-duration", ""));
                Timestamp expiresAt = duration > 0 ? new Timestamp(System.currentTimeMillis() + duration) : null;
                freezeManager.forget(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getDatabaseManager().banPlayer(player.getName(), uuid.toString(), reason, "CONSOLE", expiresAt);
                    plugin.getDatabaseManager().unfreezePlayer(uuid.toString());
                    plugin.getDatabaseManager().logAction("CONSOLE", "CONSOLE", expiresAt != null ? "TEMPBAN" : "BAN",
                            player.getName(), "Logged out while frozen");
                });
                // Alert staff
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("Axentra.freeze")) {
                        staff.sendMessage(getMessage("freeze-logout-banned")
                                .replace("%player%", player.getName()));
                    }
                }
                break;
            case "NONE":
                break;
            case "ALERT":
            default:
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("Axentra.freeze")) {
                        staff.sendMessage(getMessage("freeze-logout-alert")
                                .replace("%player%", player.getName()));
                    }
                }
                break;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) freezeManager.handleJoin(player);
            }, 5L);
        }
    }
}
