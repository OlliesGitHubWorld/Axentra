package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.SnapshotManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class ProtectionListener implements Listener {

    private final App plugin;

    public ProtectionListener(App plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Session tracking
            if (isModuleEnabled("snapshots") || isModuleEnabled("trust") || isModuleEnabled("alts") || isModuleEnabled("timeline")) {
                plugin.getDatabaseManager().startPlayerSession(uuid, ip);
                plugin.getDatabaseManager().trackIp(uuid, ip);
                plugin.getDatabaseManager().trackName(uuid, player.getName());
            }

            // Trust init
            if (isModuleEnabled("trust")) {
                plugin.getTrustManager().initPlayer(uuid);
                plugin.getTrustManager().checkAutoUpgrade(uuid);
            }

            // Join snapshot
            if (isModuleEnabled("snapshots") && plugin.getFileManager().getProtection().getBoolean("snapshots.triggers.join", true)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getSnapshotManager().createSnapshot(player, "JOIN", null);
                    plugin.getSnapshotManager().cleanupSnapshots(uuid);
                });
            }

            // Apply pending restores
            if (isModuleEnabled("snapshots")) {
                List<DatabaseManager.PendingRestore> pending = plugin.getDatabaseManager().getPendingRestores(uuid);
                if (!pending.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (DatabaseManager.PendingRestore restore : pending) {
                            DatabaseManager.SnapshotInfo snapshot = plugin.getDatabaseManager().getSnapshot(restore.snapshotId);
                            if (snapshot != null) {
                                if ("INVENTORY".equals(restore.type)) {
                                    plugin.getSnapshotManager().restoreInventory(player, snapshot);
                                } else if ("ENDERCHEST".equals(restore.type)) {
                                    plugin.getSnapshotManager().restoreEnderchest(player, snapshot);
                                }
                                String msg = getMessage("restoreinv-pending-applied")
                                        .replace("%staff%", restore.restoredBy);
                                player.sendMessage(msg);
                            }
                            plugin.getDatabaseManager().removePendingRestore(restore.id);
                        }
                    });
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Quit snapshot (sync, before player data is lost)
        if (isModuleEnabled("snapshots") && plugin.getFileManager().getProtection().getBoolean("snapshots.triggers.quit", true)) {
            plugin.getSnapshotManager().createSnapshot(player, "QUIT", null);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().endPlayerSession(uuid);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!isModuleEnabled("snapshots")) return;
        if (!plugin.getFileManager().getProtection().getBoolean("snapshots.triggers.death", true)) return;

        Player player = event.getEntity();
        String uuid = player.getUniqueId().toString();
        String cause = event.getDeathMessage() != null ? event.getDeathMessage() : "Unknown";

        int snapshotId = plugin.getSnapshotManager().createSnapshot(player, "DEATH", cause);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().recordDeath(uuid, cause,
                    player.getLocation().getWorld() != null ? player.getLocation().getWorld().getName() : "unknown",
                    player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
                    snapshotId);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!isModuleEnabled("snapshots")) return;
        if (!plugin.getFileManager().getProtection().getBoolean("snapshots.triggers.world-change", true)) return;

        Player player = event.getPlayer();
        plugin.getSnapshotManager().createSnapshot(player, "WORLD_CHANGE", null);
    }

    private boolean isModuleEnabled(String module) {
        return plugin.getFileManager().getSettings().getBoolean("enabled-modules." + module, true);
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "");
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
