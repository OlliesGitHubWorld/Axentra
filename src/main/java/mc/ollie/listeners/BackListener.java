package mc.ollie.listeners;

import mc.ollie.command.BackCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class BackListener implements Listener {

    private final BackCommand backCommand;

    public BackListener(BackCommand backCommand) {
        this.backCommand = backCommand;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        TeleportCause cause = event.getCause();
        // Only track meaningful teleports, not things like getting on a horse
        if (cause == TeleportCause.PLUGIN
                || cause == TeleportCause.COMMAND
                || cause == TeleportCause.NETHER_PORTAL
                || cause == TeleportCause.END_PORTAL
                || cause == TeleportCause.END_GATEWAY
                || cause == TeleportCause.ENDER_PEARL) {
            backCommand.saveLocation(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        backCommand.saveLocation(player);
    }
}