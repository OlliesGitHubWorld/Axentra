package mc.ollie.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * Base for Axentra's own events. They are normal Bukkit events, so other plugins can listen to
 * them with {@code @EventHandler} like any other event.
 *
 * <p>An event is async when it is fired off the main thread (for example after a database write
 * on a worker thread). Check {@link #isAsynchronous()} before touching the Bukkit API in a listener.
 *
 * <p>Use {@link #fire(AxentraEvent)} to call them; it is safe from any thread.
 */
public abstract class AxentraEvent extends Event {

    protected AxentraEvent() {
        super(!Bukkit.isPrimaryThread());
    }

    /** Calls the event on the current thread. Errors in listeners never break the caller. */
    public static void fire(AxentraEvent event) {
        try {
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Axentra] A listener for " + event.getEventName() + " failed: " + e.getMessage());
        }
    }
}
