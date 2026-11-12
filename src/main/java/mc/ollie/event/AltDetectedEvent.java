package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a joining player shares an IP with other accounts. Never contains IP addresses.
 */
public class AltDetectedEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String playerUuid;
    private final String playerName;
    private final int altCount;
    private final int bannedAlts;

    public AltDetectedEvent(String playerUuid, String playerName, int altCount, int bannedAlts) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.altCount = altCount;
        this.bannedAlts = bannedAlts;
    }

    public String getPlayerUuid() { return playerUuid; }

    public String getPlayerName() { return playerName; }

    public int getAltCount() { return altCount; }

    public int getBannedAlts() { return bannedAlts; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
