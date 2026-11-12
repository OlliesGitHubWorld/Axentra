package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a report is claimed or closed. {@code change} is CLAIMED or CLOSED; {@code linkedCaseId} is 0 when none.
 */
public class ReportUpdatedEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int reportId;
    private final String change;
    private final String staffName;
    private final int linkedCaseId;

    public ReportUpdatedEvent(int reportId, String change, String staffName, int linkedCaseId) {
        this.reportId = reportId;
        this.change = change;
        this.staffName = staffName;
        this.linkedCaseId = linkedCaseId;
    }

    public int getReportId() { return reportId; }

    public String getChange() { return change; }

    public String getStaffName() { return staffName; }

    public int getLinkedCaseId() { return linkedCaseId; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
