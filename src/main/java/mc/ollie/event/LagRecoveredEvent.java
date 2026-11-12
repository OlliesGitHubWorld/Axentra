package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when TPS is back above the alert threshold after a lag alert.
 */
public class LagRecoveredEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final double tps;

    public LagRecoveredEvent(double tps) {
        this.tps = tps;
    }

    public double getTps() { return tps; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
