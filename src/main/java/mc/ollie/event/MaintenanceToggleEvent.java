package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when maintenance mode starts or ends. {@code durationMinutes} is 0 for no time limit.
 */
public class MaintenanceToggleEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean enabled;
    private final String activatedBy;
    private final String reason;
    private final int durationMinutes;

    public MaintenanceToggleEvent(boolean enabled, String activatedBy, String reason, int durationMinutes) {
        this.enabled = enabled;
        this.activatedBy = activatedBy;
        this.reason = reason;
        this.durationMinutes = durationMinutes;
    }

    public boolean isEnabled() { return enabled; }

    public String getActivatedBy() { return activatedBy; }

    public String getReason() { return reason; }

    public int getDurationMinutes() { return durationMinutes; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
