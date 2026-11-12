package mc.ollie.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a moderation case is saved (ban, tempban, mute, tempmute, warn, kick).
 * {@code source} is INGAME, DISCORD, CONSOLE or AUTO (warn thresholds).
 */
public class CaseCreatedEvent extends AxentraEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int caseId;
    private final String type;
    private final String playerName;
    private final String playerUuid;
    private final String reason;
    private final String issuedBy;
    private final long expiresAt;
    private final String source;
    private final String chatLog;

    public CaseCreatedEvent(int caseId, String type, String playerName, String playerUuid, String reason, String issuedBy, long expiresAt, String source, String chatLog) {
        this.caseId = caseId;
        this.type = type;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.reason = reason;
        this.issuedBy = issuedBy;
        this.expiresAt = expiresAt;
        this.source = source;
        this.chatLog = chatLog;
    }

    public int getCaseId() { return caseId; }

    public String getType() { return type; }

    public String getPlayerName() { return playerName; }

    public String getPlayerUuid() { return playerUuid; }

    public String getReason() { return reason; }

    public String getIssuedBy() { return issuedBy; }

    public long getExpiresAt() { return expiresAt; }

    public String getSource() { return source; }

    public String getChatLog() { return chatLog; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
