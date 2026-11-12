package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when TPS has stayed below the alert threshold long enough to alert staff.
 * {@code heaviestChunk} is "world x, z" or null.
 */
public class LagAlertEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final double tps;
    private final int durationSeconds;
    private final int healthScore;
    private final String heaviestChunk;

    public LagAlertEvent(double tps, int durationSeconds, int healthScore, String heaviestChunk) {
        this.tps = tps;
        this.durationSeconds = durationSeconds;
        this.healthScore = healthScore;
        this.heaviestChunk = heaviestChunk;
    }

    public double getTps() { return tps; }

    public int getDurationSeconds() { return durationSeconds; }

    public int getHealthScore() { return healthScore; }

    public String getHeaviestChunk() { return heaviestChunk; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
