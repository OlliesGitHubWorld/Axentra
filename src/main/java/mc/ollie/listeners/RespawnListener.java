package mc.ollie.listeners;

import mc.ollie.command.SpawnCommand;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {

    private final SpawnCommand spawnCommand;

    public RespawnListener(SpawnCommand spawnCommand) {
        this.spawnCommand = spawnCommand;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
            Location spawn = spawnCommand.getSpawnLocation();
            event.setRespawnLocation(spawn);
        }
    }
}
