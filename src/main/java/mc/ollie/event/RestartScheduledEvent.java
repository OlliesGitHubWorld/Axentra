package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a restart countdown starts (manual, scheduled, daily or auto on lag).
 */
public class RestartScheduledEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int seconds;

    public RestartScheduledEvent(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() { return seconds; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
