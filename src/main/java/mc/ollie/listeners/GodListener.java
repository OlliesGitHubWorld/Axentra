package mc.ollie.listeners;

import mc.ollie.command.GodCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class GodListener implements Listener {

    private final GodCommand godCommand;

    public GodListener(GodCommand godCommand) {
        this.godCommand = godCommand;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (godCommand.isGod(player)) {
            event.setCancelled(true);
        }
    }
}