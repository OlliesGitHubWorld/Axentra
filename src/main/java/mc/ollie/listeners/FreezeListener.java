package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener implements Listener {

    private final StaffmodeListener staffmodeListener;

    public FreezeListener(StaffmodeListener staffmodeListener) {
        this.staffmodeListener = staffmodeListener;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (staffmodeListener.isFrozen(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setTo(event.getFrom());
            }
        }
    }
}
