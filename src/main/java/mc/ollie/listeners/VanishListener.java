package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.command.VanishCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VanishListener implements Listener {

    private final App plugin;
    private final VanishCommand vanishCommand;

    public VanishListener(App plugin, VanishCommand vanishCommand) {
        this.plugin = plugin;
        this.vanishCommand = vanishCommand;
    }

    private boolean cfg(String key) {
        return plugin.getFileManager().getSettings().getBoolean(key, true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!vanishCommand.isVanished(online) || online.equals(joining)) continue;
            if (joining.hasPermission("Axentra.vanish.see")) {
                if (cfg("vanish-ghost-effect")) {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> vanishCommand.sendFakeSpectatorMode(joining, online));
                }
            } else {
                joining.hidePlayer(plugin, online);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (vanishCommand.isVanished(player)) {
            // Suppress the real quit message — a fake one was already shown when they vanished
            event.setQuitMessage(null);
            vanishCommand.cleanupOnQuit(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && vanishCommand.isVanished(player) && cfg("vanish-god-mode")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && vanishCommand.isVanished(player) && cfg("vanish-no-pickup")) {
            event.setCancelled(true);
        }
    }
}