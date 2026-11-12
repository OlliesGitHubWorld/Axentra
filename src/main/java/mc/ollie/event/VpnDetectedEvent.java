package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a joining player connects through a VPN or proxy. Never contains the IP address.
 */
public class VpnDetectedEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String playerUuid;
    private final String playerName;
    private final boolean kicked;

    public VpnDetectedEvent(String playerUuid, String playerName, boolean kicked) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.kicked = kicked;
    }

    public String getPlayerUuid() { return playerUuid; }

    public String getPlayerName() { return playerName; }

    public boolean isKicked() { return kicked; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
