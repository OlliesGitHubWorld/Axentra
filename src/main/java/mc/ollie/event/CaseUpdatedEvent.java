package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an existing case changes. {@code change} is RESOLVED, DELETED, UNBANNED, UNMUTED,
 * APPEALED or REDUCED. Load the case with DatabaseManager#getCase off the main thread if you need it.
 */
public class CaseUpdatedEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int caseId;
    private final String change;
    private final String actor;

    public CaseUpdatedEvent(int caseId, String change, String actor) {
        this.caseId = caseId;
        this.change = change;
        this.actor = actor;
    }

    public int getCaseId() { return caseId; }

    public String getChange() { return change; }

    public String getActor() { return actor; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
