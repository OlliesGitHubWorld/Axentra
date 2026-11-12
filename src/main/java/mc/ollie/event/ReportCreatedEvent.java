package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player report is saved. {@code location} is "world x, y, z" of the target, or null when offline.
 */
public class ReportCreatedEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int reportId;
    private final String targetName;
    private final String targetUuid;
    private final String reporterName;
    private final String reporterUuid;
    private final String reason;
    private final String location;
    private final String chatLog;

    public ReportCreatedEvent(int reportId, String targetName, String targetUuid, String reporterName, String reporterUuid, String reason, String location, String chatLog) {
        this.reportId = reportId;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        this.reporterName = reporterName;
        this.reporterUuid = reporterUuid;
        this.reason = reason;
        this.location = location;
        this.chatLog = chatLog;
    }

    public int getReportId() { return reportId; }

    public String getTargetName() { return targetName; }

    public String getTargetUuid() { return targetUuid; }

    public String getReporterName() { return reporterName; }

    public String getReporterUuid() { return reporterUuid; }

    public String getReason() { return reason; }

    public String getLocation() { return location; }

    public String getChatLog() { return chatLog; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
