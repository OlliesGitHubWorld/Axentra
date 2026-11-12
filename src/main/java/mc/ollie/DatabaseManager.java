package mc.ollie;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final App plugin;
    private Connection connection;

    // player uuid -> mute expiry millis (Long.MAX_VALUE = permanent, 0 = not muted)
    private final Map<String, Long> muteCache = new ConcurrentHashMap<>();

    private static final long NAME_CACHE_MS = 30_000;
    private volatile List<String> knownNamesCache = new ArrayList<>();
    private volatile long knownNamesCachedAt = 0;
    private volatile List<String> warnedNamesCache = new ArrayList<>();
    private volatile long warnedNamesCachedAt = 0;

    public DatabaseManager(App plugin) {
        this.plugin = plugin;
    }

    // ── Data classes ────────────────────────────────────────

    public static class BanInfo {
        public final String playerName;
        public final String playerUuid;
        public final String reason;
        public final String bannedBy;
        public final Timestamp expiresAt;

        public BanInfo(String playerName, String playerUuid, String reason, String bannedBy, Timestamp expiresAt) {
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.expiresAt = expiresAt;
        }
    }

    public static class IpBanInfo {
        public final String ip;
        public final String reason;
        public final String bannedBy;
        public final Timestamp expiresAt;

        public IpBanInfo(String ip, String reason, String bannedBy, Timestamp expiresAt) {
            this.ip = ip;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.expiresAt = expiresAt;
        }
    }

    public static class WarnInfo {
        public final String reason;
        public final String warnedBy;
        public final String warnedAt;

        public WarnInfo(String reason, String warnedBy, String warnedAt) {
            this.reason = reason;
            this.warnedBy = warnedBy;
            this.warnedAt = warnedAt;
        }
    }

    public static class CaseInfo {
        public final int id;
        public final String type;
        public final String playerName;
        public final String playerUuid;
        public final String reason;
        public final String issuedBy;
        public final String issuedAt;
        public final String expiresAt;
        public final boolean active;
        public final String chatLog;
        /** INGAME, DISCORD, CONSOLE or AUTO (v1.9.0). */
        public final String source;
        /** Outcome after the case ended: UNBANNED, UNMUTED, APPEALED, REDUCED, or null (v1.9.0). */
        public final String status;

        public CaseInfo(int id, String type, String playerName, String playerUuid,
                        String reason, String issuedBy, String issuedAt, String expiresAt,
                        boolean active, String chatLog) {
            this(id, type, playerName, playerUuid, reason, issuedBy, issuedAt, expiresAt, active, chatLog, "INGAME", null);
        }

        public CaseInfo(int id, String type, String playerName, String playerUuid,
                        String reason, String issuedBy, String issuedAt, String expiresAt,
                        boolean active, String chatLog, String source, String status) {
            this.source = source != null ? source : "INGAME";
            this.status = status;
            this.id = id;
            this.type = type;
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.reason = reason;
            this.issuedBy = issuedBy;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.active = active;
            this.chatLog = chatLog;
        }
    }

    public static class NoteInfo {
        public final int id;
        public final String playerName;
        public final String playerUuid;
        public final String note;
        public final String addedBy;
        public final String addedAt;

        public NoteInfo(int id, String playerName, String playerUuid, String note, String addedBy, String addedAt) {
            this.id = id;
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.note = note;
            this.addedBy = addedBy;
            this.addedAt = addedAt;
        }
    }

    public static class AuditEntry {
        public final int id;
        public final String staffName;
        public final String staffUuid;
        public final String action;
        public final String target;
        public final String details;
        public final String timestamp;

        public AuditEntry(int id, String staffName, String staffUuid, String action,
                          String target, String details, String timestamp) {
            this.id = id;
            this.staffName = staffName;
            this.staffUuid = staffUuid;
            this.action = action;
            this.target = target;
            this.details = details;
            this.timestamp = timestamp;
        }
    }

    public static class ReportInfo {
        public final int id;
        public final String targetName;
        public final String targetUuid;
        public final String reason;
        public final String reporterName;
        public final String reporterUuid;
        public final String reportedAt;
        public final String status; // OPEN, CLAIMED, CLOSED
        public final String claimedBy;
        public final String closedBy;
        public final Integer linkedCaseId;
        public final String chatLog;

        public ReportInfo(int id, String targetName, String targetUuid, String reason,
                         String reporterName, String reporterUuid, String reportedAt,
                         String status, String claimedBy, String closedBy, Integer linkedCaseId, String chatLog) {
            this.id = id;
            this.targetName = targetName;
            this.targetUuid = targetUuid;
            this.reason = reason;
            this.reporterName = reporterName;
            this.reporterUuid = reporterUuid;
            this.reportedAt = reportedAt;
            this.status = status;
            this.claimedBy = claimedBy;
            this.closedBy = closedBy;
            this.linkedCaseId = linkedCaseId;
            this.chatLog = chatLog;
        }
    }

    public static class StaffStat {
        public final String staffName;
        public final int totalCases;
        public final int reportsClosed;
        public final long dutyMinutes;

        public StaffStat(String staffName, int totalCases, int reportsClosed, long dutyMinutes) {
            this.staffName = staffName;
            this.totalCases = totalCases;
            this.reportsClosed = reportsClosed;
            this.dutyMinutes = dutyMinutes;
        }
    }

    public static class LeaderboardEntry {
        public final String staffName;
        public final int caseCount;

        public LeaderboardEntry(String staffName, int caseCount) {
            this.staffName = staffName;
            this.caseCount = caseCount;
        }
    }

    // ── v1.7.0 Data classes ─────────────────────────────────

    public static class SessionInfo {
        public final int id;
        public final String playerUuid;
        public final String ip;
        public final long joinTime;
        public final long quitTime;

        public SessionInfo(int id, String playerUuid, String ip, long joinTime, long quitTime) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.ip = ip;
            this.joinTime = joinTime;
            this.quitTime = quitTime;
        }
    }

    public static class IpHistoryEntry {
        public final String playerUuid;
        public final String ip;
        public final long firstSeen;
        public final long lastSeen;

        public IpHistoryEntry(String playerUuid, String ip, long firstSeen, long lastSeen) {
            this.playerUuid = playerUuid;
            this.ip = ip;
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
        }
    }

    public static class NameHistoryEntry {
        public final String playerUuid;
        public final String name;
        public final long changedAt;

        public NameHistoryEntry(String playerUuid, String name, long changedAt) {
            this.playerUuid = playerUuid;
            this.name = name;
            this.changedAt = changedAt;
        }
    }

    public static class DeathInfo {
        public final int id;
        public final String playerUuid;
        public final String cause;
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public final long time;
        public final int snapshotId;

        public DeathInfo(int id, String playerUuid, String cause, String world,
                        double x, double y, double z, long time, int snapshotId) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.cause = cause;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
            this.snapshotId = snapshotId;
        }
    }

    public static class SnapshotInfo {
        public final int id;
        public final String playerUuid;
        public final String playerName;
        public final String inventoryData;
        public final String armorData;
        public final String offhandData;
        public final String enderchestData;
        public final int xpLevel;
        public final float xpProgress;
        public final double health;
        public final int food;
        public final String gamemode;
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public final String reason;
        public final long time;
        public final String deathCause;

        public SnapshotInfo(int id, String playerUuid, String playerName,
                           String inventoryData, String armorData, String offhandData,
                           String enderchestData, int xpLevel, float xpProgress,
                           double health, int food, String gamemode,
                           String world, double x, double y, double z,
                           String reason, long time, String deathCause) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.inventoryData = inventoryData;
            this.armorData = armorData;
            this.offhandData = offhandData;
            this.enderchestData = enderchestData;
            this.xpLevel = xpLevel;
            this.xpProgress = xpProgress;
            this.health = health;
            this.food = food;
            this.gamemode = gamemode;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.reason = reason;
            this.time = time;
            this.deathCause = deathCause;
        }
    }

    public static class PendingRestore {
        public final int id;
        public final String playerUuid;
        public final int snapshotId;
        public final String type;
        public final String restoredBy;

        public PendingRestore(int id, String playerUuid, int snapshotId, String type, String restoredBy) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.snapshotId = snapshotId;
            this.type = type;
            this.restoredBy = restoredBy;
        }
    }

    public static class TrustInfo {
        public final String playerUuid;
        public final String level;
        public final boolean manual;
        public final long updatedAt;

        public TrustInfo(String playerUuid, String level, boolean manual, long updatedAt) {
            this.playerUuid = playerUuid;
            this.level = level;
            this.manual = manual;
            this.updatedAt = updatedAt;
        }
    }

    public static class TrustHistoryEntry {
        public final int id;
        public final String playerUuid;
        public final String oldLevel;
        public final String newLevel;
        public final String reason;
        public final String changedBy;
        public final long time;

        public TrustHistoryEntry(int id, String playerUuid, String oldLevel, String newLevel,
                                String reason, String changedBy, long time) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.reason = reason;
            this.changedBy = changedBy;
            this.time = time;
        }
    }

    public static class VpnCacheEntry {
        public final String ip;
        public final boolean isVpn;
        public final long expiresAt;

        public VpnCacheEntry(String ip, boolean isVpn, long expiresAt) {
            this.ip = ip;
            this.isVpn = isVpn;
            this.expiresAt = expiresAt;
        }
    }

    public static class HomeInfo {
        public final int id;
        public final String playerUuid;
        public final String name;
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;

        public HomeInfo(int id, String playerUuid, String name, String world,
                       double x, double y, double z, float yaw, float pitch) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static class AltInfo {
        public final String playerUuid;
        public final String playerName;
        public final boolean online;
        public final long lastSeen;
        public final boolean banned;
        public final boolean muted;
        public final int caseCount;

        public AltInfo(String playerUuid, String playerName, boolean online, long lastSeen,
                      boolean banned, boolean muted, int caseCount) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.online = online;
            this.lastSeen = lastSeen;
            this.banned = banned;
            this.muted = muted;
            this.caseCount = caseCount;
        }
    }

    // ── v1.8.0 Data classes ─────────────────────────────────

    public static class TpsHistoryEntry {
        public final long time;
        public final double tps1m;
        public final double tps5m;
        public final double tps15m;
        public final double msptAvg;
        public final double msptMax;
        public final long memoryUsed;
        public final long memoryMax;
        public final int healthScore;
        public final int onlinePlayers;

        public TpsHistoryEntry(long time, double tps1m, double tps5m, double tps15m,
                              double msptAvg, double msptMax, long memoryUsed, long memoryMax,
                              int healthScore, int onlinePlayers) {
            this.time = time;
            this.tps1m = tps1m;
            this.tps5m = tps5m;
            this.tps15m = tps15m;
            this.msptAvg = msptAvg;
            this.msptMax = msptMax;
            this.memoryUsed = memoryUsed;
            this.memoryMax = memoryMax;
            this.healthScore = healthScore;
            this.onlinePlayers = onlinePlayers;
        }
    }

    public static class LagAlertInfo {
        public final int id;
        public final long time;
        public final double tps;
        public final int durationSeconds;
        public final int healthScore;
        public final boolean resolved;
        public final long resolvedAt;

        public LagAlertInfo(int id, long time, double tps, int durationSeconds,
                           int healthScore, boolean resolved, long resolvedAt) {
            this.id = id;
            this.time = time;
            this.tps = tps;
            this.durationSeconds = durationSeconds;
            this.healthScore = healthScore;
            this.resolved = resolved;
            this.resolvedAt = resolvedAt;
        }
    }

    public static class LagAlertChunkInfo {
        public final int id;
        public final int alertId;
        public final String world;
        public final int chunkX;
        public final int chunkZ;
        public final double score;
        public final int entityCount;
        public final int tileEntityCount;
        public final String topContributorUuid;
        public final String topContributorName;

        public LagAlertChunkInfo(int id, int alertId, String world, int chunkX, int chunkZ,
                                double score, int entityCount, int tileEntityCount,
                                String topContributorUuid, String topContributorName) {
            this.id = id;
            this.alertId = alertId;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.score = score;
            this.entityCount = entityCount;
            this.tileEntityCount = tileEntityCount;
            this.topContributorUuid = topContributorUuid;
            this.topContributorName = topContributorName;
        }
    }

    public static class ChunkActivityEntry {
        public final int id;
        public final String playerUuid;
        public final String playerName;
        public final String world;
        public final int chunkX;
        public final int chunkZ;
        public final String actionType;
        public final String detail;
        public final int count;
        public final long time;

        public ChunkActivityEntry(int id, String playerUuid, String playerName,
                                 String world, int chunkX, int chunkZ,
                                 String actionType, String detail, int count, long time) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.actionType = actionType;
            this.detail = detail;
            this.count = count;
            this.time = time;
        }
    }

    public static class WarpInfo {
        public final int id;
        public final String name;
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;
        public final String icon;
        public final String createdBy;
        public final long createdAt;
        public final String permission;

        public WarpInfo(int id, String name, String world, double x, double y, double z,
                       float yaw, float pitch, String icon, String createdBy, long createdAt,
                       String permission) {
            this.id = id;
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.icon = icon;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.permission = permission;
        }
    }

    public static class MaintenanceInfo {
        public final boolean active;
        public final String activatedBy;
        public final long activatedAt;
        public final int durationMinutes;
        public final String reason;

        public MaintenanceInfo(boolean active, String activatedBy, long activatedAt,
                              int durationMinutes, String reason) {
            this.active = active;
            this.activatedBy = activatedBy;
            this.activatedAt = activatedAt;
            this.durationMinutes = durationMinutes;
            this.reason = reason;
        }
    }

    // ── Setup ───────────────────────────────────────────────

    public void setup() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // WAL lets reads and writes from different threads run side by side, and busy_timeout
            // waits for a lock instead of failing with "database is locked" when it's busy.
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA journal_mode=WAL");
                pragma.execute("PRAGMA synchronous=NORMAL");
                pragma.execute("PRAGMA busy_timeout=5000");
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS bans (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_name TEXT NOT NULL," +
                                "player_uuid TEXT NOT NULL," +
                                "reason TEXT," +
                                "banned_by TEXT NOT NULL," +
                                "banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "expires_at TIMESTAMP," +
                                "active INTEGER DEFAULT 1" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS ip_bans (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "ip TEXT NOT NULL," +
                                "reason TEXT," +
                                "banned_by TEXT NOT NULL," +
                                "banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "expires_at TIMESTAMP," +
                                "active INTEGER DEFAULT 1" +
                                ")"
                );

                // Add expires_at to existing ip_bans tables that predate this column
                try {
                    statement.executeUpdate("ALTER TABLE ip_bans ADD COLUMN expires_at TIMESTAMP");
                } catch (SQLException ignored) { }

                // Add expires_at to existing mutes tables that predate this column
                try {
                    statement.executeUpdate("ALTER TABLE mutes ADD COLUMN expires_at TIMESTAMP");
                } catch (SQLException ignored) { }

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS warns (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_name TEXT NOT NULL," +
                                "player_uuid TEXT NOT NULL," +
                                "reason TEXT," +
                                "warned_by TEXT NOT NULL," +
                                "warned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "active INTEGER DEFAULT 1" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS mutes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_name TEXT NOT NULL," +
                                "player_uuid TEXT NOT NULL," +
                                "reason TEXT," +
                                "muted_by TEXT NOT NULL," +
                                "muted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "active INTEGER DEFAULT 1" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS nicknames (" +
                                "player_uuid TEXT PRIMARY KEY," +
                                "nickname TEXT NOT NULL" +
                                ")"
                );

                // ── Smart Moderation tables (v1.5.0) ──

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS cases (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "type TEXT NOT NULL," +
                                "player_name TEXT NOT NULL," +
                                "player_uuid TEXT NOT NULL," +
                                "reason TEXT," +
                                "issued_by TEXT NOT NULL," +
                                "issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "expires_at TIMESTAMP," +
                                "active INTEGER DEFAULT 1," +
                                "chat_log TEXT" +
                                ")"
                );

                // v1.9.0: where a case came from (INGAME/DISCORD/CONSOLE/AUTO) and its outcome (UNBANNED, APPEALED, ...)
                try {
                    statement.executeUpdate("ALTER TABLE cases ADD COLUMN source TEXT DEFAULT 'INGAME'");
                } catch (SQLException ignored) { }
                try {
                    statement.executeUpdate("ALTER TABLE cases ADD COLUMN status TEXT");
                } catch (SQLException ignored) { }

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS staff_notes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_name TEXT NOT NULL," +
                                "player_uuid TEXT NOT NULL," +
                                "note TEXT NOT NULL," +
                                "added_by TEXT NOT NULL," +
                                "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS audit_log (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "staff_name TEXT NOT NULL," +
                                "staff_uuid TEXT NOT NULL," +
                                "action TEXT NOT NULL," +
                                "target TEXT," +
                                "details TEXT," +
                                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ")"
                );

                // ── Staff Management tables (v1.6.0) ──

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS reports (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "target_name TEXT NOT NULL," +
                                "target_uuid TEXT NOT NULL," +
                                "reason TEXT," +
                                "reporter_name TEXT NOT NULL," +
                                "reporter_uuid TEXT NOT NULL," +
                                "reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "status TEXT DEFAULT 'OPEN'," +
                                "claimed_by TEXT," +
                                "closed_by TEXT," +
                                "linked_case_id INTEGER," +
                                "chat_log TEXT" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS staff_sessions (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "staff_uuid TEXT NOT NULL," +
                                "started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "ended_at TIMESTAMP" +
                                ")"
                );

                // ── Player Protection tables (v1.7.0) ──

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_sessions (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "ip TEXT," +
                                "join_time INTEGER NOT NULL," +
                                "quit_time INTEGER" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS ip_history (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "ip TEXT NOT NULL," +
                                "first_seen INTEGER NOT NULL," +
                                "last_seen INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS name_history (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "name TEXT NOT NULL," +
                                "changed_at INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_deaths (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "cause TEXT," +
                                "world TEXT," +
                                "x REAL," +
                                "y REAL," +
                                "z REAL," +
                                "time INTEGER NOT NULL," +
                                "snapshot_id INTEGER" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS inventory_snapshots (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "player_name TEXT NOT NULL," +
                                "inventory_data TEXT," +
                                "armor_data TEXT," +
                                "offhand_data TEXT," +
                                "enderchest_data TEXT," +
                                "xp_level INTEGER," +
                                "xp_progress REAL," +
                                "health REAL," +
                                "food INTEGER," +
                                "gamemode TEXT," +
                                "world TEXT," +
                                "x REAL," +
                                "y REAL," +
                                "z REAL," +
                                "reason TEXT NOT NULL," +
                                "time INTEGER NOT NULL," +
                                "death_cause TEXT" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS pending_restores (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "snapshot_id INTEGER NOT NULL," +
                                "type TEXT NOT NULL," +
                                "restored_by TEXT NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_trust (" +
                                "player_uuid TEXT PRIMARY KEY," +
                                "level TEXT NOT NULL DEFAULT 'NEW'," +
                                "manual INTEGER DEFAULT 0," +
                                "updated_at INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS trust_history (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "old_level TEXT," +
                                "new_level TEXT NOT NULL," +
                                "reason TEXT," +
                                "changed_by TEXT," +
                                "time INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS vpn_cache (" +
                                "ip TEXT PRIMARY KEY," +
                                "is_vpn INTEGER NOT NULL," +
                                "expires_at INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS frozen_players (" +
                                "player_uuid TEXT PRIMARY KEY," +
                                "frozen_by TEXT NOT NULL," +
                                "frozen_at INTEGER NOT NULL" +
                                ")"
                );

                // Add expires_at to existing frozen_players tables (temp freezes, 0 = permanent)
                try {
                    statement.executeUpdate("ALTER TABLE frozen_players ADD COLUMN expires_at INTEGER DEFAULT 0");
                } catch (SQLException ignored) { }

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS homes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "name TEXT NOT NULL," +
                                "world TEXT NOT NULL," +
                                "x REAL NOT NULL," +
                                "y REAL NOT NULL," +
                                "z REAL NOT NULL," +
                                "yaw REAL NOT NULL," +
                                "pitch REAL NOT NULL" +
                                ")"
                );

                // Indexes
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sessions_uuid ON player_sessions(player_uuid)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ip_history_uuid ON ip_history(player_uuid)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ip_history_ip ON ip_history(ip)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_snapshots_uuid ON inventory_snapshots(player_uuid)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_deaths_uuid ON player_deaths(player_uuid)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_homes_uuid ON homes(player_uuid)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name_history_uuid ON name_history(player_uuid)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_trust_history_uuid ON trust_history(player_uuid)"); } catch (SQLException ignored) {}

                // ── Server Health tables (v1.8.0) ──

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS tps_history (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "time INTEGER NOT NULL," +
                                "tps_1m REAL NOT NULL," +
                                "tps_5m REAL NOT NULL," +
                                "tps_15m REAL NOT NULL," +
                                "mspt_avg REAL NOT NULL," +
                                "mspt_max REAL NOT NULL," +
                                "memory_used INTEGER NOT NULL," +
                                "memory_max INTEGER NOT NULL," +
                                "health_score INTEGER NOT NULL," +
                                "online_players INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS lag_alerts (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "time INTEGER NOT NULL," +
                                "tps REAL NOT NULL," +
                                "duration_seconds INTEGER NOT NULL," +
                                "health_score INTEGER NOT NULL," +
                                "resolved INTEGER DEFAULT 0," +
                                "resolved_at INTEGER" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS lag_alert_chunks (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "alert_id INTEGER NOT NULL," +
                                "world TEXT NOT NULL," +
                                "chunk_x INTEGER NOT NULL," +
                                "chunk_z INTEGER NOT NULL," +
                                "score REAL NOT NULL," +
                                "entity_count INTEGER NOT NULL," +
                                "tile_entity_count INTEGER NOT NULL," +
                                "top_contributor_uuid TEXT," +
                                "top_contributor_name TEXT" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS chunk_activity (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "player_uuid TEXT NOT NULL," +
                                "player_name TEXT NOT NULL," +
                                "world TEXT NOT NULL," +
                                "chunk_x INTEGER NOT NULL," +
                                "chunk_z INTEGER NOT NULL," +
                                "action_type TEXT NOT NULL," +
                                "detail TEXT," +
                                "count INTEGER NOT NULL DEFAULT 1," +
                                "time INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS warps (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "name TEXT NOT NULL UNIQUE," +
                                "world TEXT NOT NULL," +
                                "x REAL NOT NULL," +
                                "y REAL NOT NULL," +
                                "z REAL NOT NULL," +
                                "yaw REAL NOT NULL," +
                                "pitch REAL NOT NULL," +
                                "icon TEXT," +
                                "created_by TEXT NOT NULL," +
                                "created_at INTEGER NOT NULL," +
                                "permission TEXT" +
                                ")"
                );

                // Add sort_order to existing warps tables (0 = unsorted, falls back to name order)
                try {
                    statement.executeUpdate("ALTER TABLE warps ADD COLUMN sort_order INTEGER DEFAULT 0");
                } catch (SQLException ignored) { }

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS rtp_cooldowns (" +
                                "player_uuid TEXT PRIMARY KEY," +
                                "last_used INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS maintenance (" +
                                "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                                "active INTEGER NOT NULL DEFAULT 0," +
                                "activated_by TEXT," +
                                "activated_at INTEGER," +
                                "duration_minutes INTEGER," +
                                "reason TEXT" +
                                ")"
                );

                // v1.8.0 indexes
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tps_history_time ON tps_history(time)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lag_alerts_time ON lag_alerts(time)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lag_alert_chunks_alert ON lag_alert_chunks(alert_id)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chunk_activity_chunk ON chunk_activity(world, chunk_x, chunk_z)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chunk_activity_player ON chunk_activity(player_uuid)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chunk_activity_time ON chunk_activity(time)"); } catch (SQLException ignored) {}

                // ── Discord tables (v1.9.0) ──

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS discord_links (" +
                                "player_uuid TEXT NOT NULL," +
                                "player_name TEXT," +
                                "discord_id TEXT NOT NULL," +
                                "discord_name TEXT," +
                                "linked_at INTEGER NOT NULL," +
                                // Comma-separated Axentra permissions the player had on their last join,
                                // used to check offline staff that act from Discord
                                "perm_snapshot TEXT," +
                                "PRIMARY KEY (player_uuid, discord_id)" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS link_codes (" +
                                "code TEXT PRIMARY KEY," +
                                "player_uuid TEXT NOT NULL," +
                                "player_name TEXT," +
                                "expires_at INTEGER NOT NULL" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS appeals (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "case_id INTEGER NOT NULL," +
                                "player_uuid TEXT," +
                                "player_name TEXT," +
                                "discord_id TEXT NOT NULL," +
                                "answers TEXT," +
                                "status TEXT NOT NULL DEFAULT 'OPEN'," +
                                "decided_by TEXT," +
                                "decision TEXT," +
                                "created_at INTEGER NOT NULL," +
                                "decided_at INTEGER" +
                                ")"
                );

                // Which Discord message shows a case, report or appeal, so it can be edited later
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS discord_messages (" +
                                "kind TEXT NOT NULL," +
                                "ref_id INTEGER NOT NULL," +
                                "channel_id TEXT NOT NULL," +
                                "message_id TEXT NOT NULL," +
                                "PRIMARY KEY (kind, ref_id)" +
                                ")"
                );

                // AFK time per session, excluded from playtime and trust progression
                try {
                    statement.executeUpdate("ALTER TABLE player_sessions ADD COLUMN afk_seconds INTEGER DEFAULT 0");
                } catch (SQLException ignored) { }

                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_discord_links_discord ON discord_links(discord_id)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_appeals_case ON appeals(case_id)"); } catch (SQLException ignored) {}
                try { statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cases_issued ON cases(issued_at)"); } catch (SQLException ignored) {}
            }

            plugin.logInfo("Database connected!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }

    // ── Bans ────────────────────────────────────────────────

    public void banPlayer(String playerName, String playerUuid, String reason, String bannedBy, Timestamp expiresAt) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO bans (player_name, player_uuid, reason, banned_by, expires_at) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, reason);
            stmt.setString(4, bannedBy);
            stmt.setTimestamp(5, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to ban player: " + e.getMessage());
        }
    }

    public void unbanPlayer(String playerName) {
        unbanPlayer(playerName, "UNBANNED", null);
    }

    /**
     * Lifts a ban and closes the player's active ban cases with the given status
     * (UNBANNED, or APPEALED for an accepted appeal).
     */
    public void unbanPlayer(String playerName, String caseStatus, String actor) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE bans SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1")) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
            return;
        }
        for (int caseId : getActiveCaseIds(playerName, "BAN", "TEMPBAN")) {
            setCaseStatus(caseId, caseStatus, false, actor);
        }
    }

    public boolean isBannedByName(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM bans WHERE LOWER(player_name) = LOWER(?) AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, playerName);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check ban: " + e.getMessage());
            return false;
        }
    }

    public boolean isBanned(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM bans WHERE player_uuid = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check ban: " + e.getMessage());
            return false;
        }
    }

    public void banIp(String ip, String reason, String bannedBy, Timestamp expiresAt) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO ip_bans (ip, reason, banned_by, expires_at) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, ip);
            stmt.setString(2, reason);
            stmt.setString(3, bannedBy);
            stmt.setTimestamp(4, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to IP ban: " + e.getMessage());
        }
    }

    public void unbanIp(String ip) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE ip_bans SET active = 0 WHERE ip = ? AND active = 1")) {
            stmt.setString(1, ip);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unban IP: " + e.getMessage());
        }
    }

    public boolean isIpBanned(String ip) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM ip_bans WHERE ip = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, ip);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check IP ban: " + e.getMessage());
            return false;
        }
    }

    public IpBanInfo getIpBan(String ip) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM ip_bans WHERE ip = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, ip);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new IpBanInfo(
                            rs.getString("ip"),
                            rs.getString("reason"),
                            rs.getString("banned_by"),
                            rs.getTimestamp("expires_at")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get IP ban: " + e.getMessage());
        }
        return null;
    }

    // ── Warns ───────────────────────────────────────────────

    public void warnPlayer(String playerName, String playerUuid, String reason, String warnedBy) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO warns (player_name, player_uuid, reason, warned_by) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, playerName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, reason);
            stmt.setString(4, warnedBy);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to warn player: " + e.getMessage());
        }
    }

    public int getWarnCount(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM warns WHERE player_uuid = ? AND active = 1")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warn count: " + e.getMessage());
        }
        return 0;
    }

    public int getWarnCountByName(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM warns WHERE LOWER(player_name) = LOWER(?) AND active = 1")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warn count by name: " + e.getMessage());
        }
        return 0;
    }

    public List<WarnInfo> getWarnsByName(String playerName) {
        List<WarnInfo> warns = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT reason, warned_by, warned_at FROM warns WHERE LOWER(player_name) = LOWER(?) AND active = 1 ORDER BY warned_at ASC")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    warns.add(new WarnInfo(
                            rs.getString("reason"),
                            rs.getString("warned_by"),
                            rs.getString("warned_at")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warns: " + e.getMessage());
        }
        return warns;
    }

    public void clearWarns(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE warns SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1")) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clear warns: " + e.getMessage());
        }
    }

    // ── Mutes ───────────────────────────────────────────────

    public void mutePlayer(String playerName, String playerUuid, String reason, String mutedBy, Timestamp expiresAt) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO mutes (player_name, player_uuid, reason, muted_by, expires_at) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, reason);
            stmt.setString(4, mutedBy);
            stmt.setTimestamp(5, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to mute player: " + e.getMessage());
        }
        muteCache.remove(playerUuid);
    }

    public void unmutePlayer(String playerName) {
        unmutePlayer(playerName, "UNMUTED", null);
    }

    /** Lifts a mute and closes the player's active mute cases with the given status. */
    public void unmutePlayer(String playerName, String caseStatus, String actor) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE mutes SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1")) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unmute player: " + e.getMessage());
        }
        // Unmute goes by name, so we can't map it to one UUID; unmutes are rare, just reload everyone
        muteCache.clear();
        for (int caseId : getActiveCaseIds(playerName, "MUTE", "TEMPMUTE")) {
            setCaseStatus(caseId, caseStatus, false, actor);
        }
    }

    /**
     * Cached: checked on every chat message, so only the first check per player hits the DB.
     * The cache is invalidated by mutePlayer/unmutePlayer.
     */
    public boolean isMuted(String playerUuid) {
        Long expiry = muteCache.get(playerUuid);
        if (expiry == null) {
            expiry = loadMuteExpiry(playerUuid);
            muteCache.put(playerUuid, expiry);
        }
        return expiry > System.currentTimeMillis();
    }

    /** Returns the mute expiry in millis, Long.MAX_VALUE for permanent, 0 when not muted. */
    private long loadMuteExpiry(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT expires_at FROM mutes WHERE player_uuid = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            long expiry = 0;
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    expiry = Math.max(expiry, expiresAt == null ? Long.MAX_VALUE : expiresAt.getTime());
                }
            }
            return expiry;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check mute: " + e.getMessage());
            return 0;
        }
    }

    /** Drops a player's cached mute state, e.g. when they leave. */
    public void forgetMute(String playerUuid) {
        muteCache.remove(playerUuid);
    }

    public boolean isMutedByName(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM mutes WHERE LOWER(player_name) = LOWER(?) AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, playerName);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check mute by name: " + e.getMessage());
            return false;
        }
    }

    // ── Name lists ──────────────────────────────────────────

    public List<String> getActiveBannedNames() {
        List<String> names = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT player_name FROM bans WHERE active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) names.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get banned names: " + e.getMessage());
        }
        return names;
    }

    public List<String> getActiveMutedNames() {
        List<String> names = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT player_name FROM mutes WHERE active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) names.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get muted names: " + e.getMessage());
        }
        return names;
    }

    /** Used for tab completion, so cached briefly instead of querying on every keystroke. */
    public List<String> getActiveWarnedNames() {
        if (System.currentTimeMillis() - warnedNamesCachedAt < NAME_CACHE_MS) return warnedNamesCache;
        List<String> names = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT player_name FROM warns WHERE active = 1")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) names.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warned names: " + e.getMessage());
        }
        warnedNamesCache = names;
        warnedNamesCachedAt = System.currentTimeMillis();
        return names;
    }

    // ── Nicknames ───────────────────────────────────────────

    public void setNickname(String playerUuid, String nickname) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO nicknames (player_uuid, nickname) VALUES (?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, nickname);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set nickname: " + e.getMessage());
        }
    }

    public void removeNickname(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM nicknames WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove nickname: " + e.getMessage());
        }
    }

    public String getNickname(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT nickname FROM nicknames WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("nickname");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get nickname: " + e.getMessage());
        }
        return null;
    }

    public BanInfo getBan(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM bans WHERE player_uuid = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new BanInfo(
                            rs.getString("player_name"),
                            rs.getString("player_uuid"),
                            rs.getString("reason"),
                            rs.getString("banned_by"),
                            rs.getTimestamp("expires_at")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get ban: " + e.getMessage());
        }
        return null;
    }

    // ── Cases (v1.5.0) ─────────────────────────────────────

    public int createCase(String type, String playerName, String playerUuid,
                          String reason, String issuedBy, Timestamp expiresAt, String chatLog) {
        String source = "CONSOLE".equalsIgnoreCase(issuedBy) ? "CONSOLE" : "INGAME";
        return createCase(type, playerName, playerUuid, reason, issuedBy, expiresAt, chatLog, source);
    }

    /**
     * Saves a case and fires {@link mc.ollie.event.CaseCreatedEvent}.
     *
     * @param source INGAME, DISCORD, CONSOLE or AUTO
     */
    public int createCase(String type, String playerName, String playerUuid,
                          String reason, String issuedBy, Timestamp expiresAt, String chatLog, String source) {
        // Kicks and warns are instant actions — mark them inactive immediately
        boolean instant = type.equals("KICK") || type.equals("WARN");
        int caseId = -1;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO cases (type, player_name, player_uuid, reason, issued_by, expires_at, chat_log, active, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, type);
            stmt.setString(2, playerName);
            stmt.setString(3, playerUuid);
            stmt.setString(4, reason);
            stmt.setString(5, issuedBy);
            stmt.setTimestamp(6, expiresAt);
            stmt.setString(7, chatLog);
            stmt.setInt(8, instant ? 0 : 1);
            stmt.setString(9, source);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) caseId = keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create case: " + e.getMessage());
        }
        if (caseId > 0) {
            mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.CaseCreatedEvent(caseId, type, playerName, playerUuid,
                    reason, issuedBy, expiresAt != null ? expiresAt.getTime() : 0, source, chatLog));
        }
        return caseId;
    }

    /** Ends a case with an outcome (UNBANNED, APPEALED, ...) and fires CaseUpdatedEvent. */
    public void setCaseStatus(int caseId, String status, boolean active, String actor) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE cases SET status = ?, active = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, active ? 1 : 0);
            stmt.setInt(3, caseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update case status: " + e.getMessage());
            return;
        }
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.CaseUpdatedEvent(caseId, status, actor));
    }

    /** Changes the end time of a case (and the matching ban or mute) and fires CaseUpdatedEvent REDUCED. */
    public void setCaseExpiry(int caseId, Timestamp expiresAt, String actor) {
        CaseInfo info = getCase(caseId);
        if (info == null) return;
        String table = info.type.contains("BAN") ? "bans" : "mutes";
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE cases SET expires_at = ?, status = 'REDUCED' WHERE id = ?");
             PreparedStatement pun = connection.prepareStatement(
                     "UPDATE " + table + " SET expires_at = ? WHERE player_uuid = ? AND active = 1")) {
            stmt.setTimestamp(1, expiresAt);
            stmt.setInt(2, caseId);
            stmt.executeUpdate();
            pun.setTimestamp(1, expiresAt);
            pun.setString(2, info.playerUuid);
            pun.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to change case expiry: " + e.getMessage());
            return;
        }
        if (table.equals("mutes")) muteCache.remove(info.playerUuid);
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.CaseUpdatedEvent(caseId, "REDUCED", actor));
    }

    /** Newest active ban case of a player (for the appeal hint on the ban screen), or -1. */
    public int getActiveBanCaseId(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id FROM cases WHERE player_uuid = ? AND type IN ('BAN','TEMPBAN') AND active = 1 ORDER BY id DESC LIMIT 1")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get active ban case: " + e.getMessage());
        }
        return -1;
    }

    /** Active ban or mute cases of a player, by name. Used to close them when the punishment is lifted. */
    private List<Integer> getActiveCaseIds(String playerName, String typeA, String typeB) {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id FROM cases WHERE LOWER(player_name) = LOWER(?) AND type IN (?, ?) AND active = 1")) {
            stmt.setString(1, playerName);
            stmt.setString(2, typeA);
            stmt.setString(3, typeB);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to look up active cases: " + e.getMessage());
        }
        return ids;
    }

    public CaseInfo getCase(int caseId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM cases WHERE id = ?")) {
            stmt.setInt(1, caseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CaseInfo(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("player_name"),
                            rs.getString("player_uuid"),
                            rs.getString("reason"),
                            rs.getString("issued_by"),
                            rs.getString("issued_at"),
                            rs.getString("expires_at"),
                            rs.getInt("active") == 1,
                            rs.getString("chat_log"),
                            rs.getString("source"), rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get case: " + e.getMessage());
        }
        return null;
    }

    public List<CaseInfo> getCasesByPlayer(String playerName) {
        List<CaseInfo> cases = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM cases WHERE LOWER(player_name) = LOWER(?) ORDER BY issued_at DESC")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cases.add(new CaseInfo(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("player_name"),
                            rs.getString("player_uuid"),
                            rs.getString("reason"),
                            rs.getString("issued_by"),
                            rs.getString("issued_at"),
                            rs.getString("expires_at"),
                            rs.getInt("active") == 1,
                            rs.getString("chat_log"),
                            rs.getString("source"), rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get cases: " + e.getMessage());
        }
        return cases;
    }

    public List<CaseInfo> getCasesByPlayerUuid(String playerUuid) {
        List<CaseInfo> cases = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM cases WHERE player_uuid = ? ORDER BY issued_at DESC")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cases.add(new CaseInfo(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("player_name"),
                            rs.getString("player_uuid"),
                            rs.getString("reason"),
                            rs.getString("issued_by"),
                            rs.getString("issued_at"),
                            rs.getString("expires_at"),
                            rs.getInt("active") == 1,
                            rs.getString("chat_log"),
                            rs.getString("source"), rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get cases by UUID: " + e.getMessage());
        }
        return cases;
    }

    /** Used for tab completion, so cached briefly instead of querying on every keystroke. */
    public List<String> getKnownPlayerNames() {
        if (System.currentTimeMillis() - knownNamesCachedAt < NAME_CACHE_MS) return knownNamesCache;
        List<String> names = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT player_name FROM cases")) {
            while (rs.next()) {
                names.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get known player names: " + e.getMessage());
        }
        knownNamesCache = names;
        knownNamesCachedAt = System.currentTimeMillis();
        return names;
    }

    /** Number of earlier cases per reason for a player, used for punishment escalation. */
    public java.util.Map<String, Integer> getCaseCountsByReason(String playerUuid) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT reason, COUNT(*) AS amount FROM cases WHERE player_uuid = ? AND reason IS NOT NULL GROUP BY reason")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) counts.put(rs.getString("reason"), rs.getInt("amount"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to count cases by reason: " + e.getMessage());
        }
        return counts;
    }

    public int getCaseCountByPlayer(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM cases WHERE LOWER(player_name) = LOWER(?) AND active = 1")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get case count: " + e.getMessage());
        }
        return 0;
    }

    public void resolveCase(int caseId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE cases SET active = 0 WHERE id = ?")) {
            stmt.setInt(1, caseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to resolve case: " + e.getMessage());
            return;
        }
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.CaseUpdatedEvent(caseId, "RESOLVED", null));
    }

    public void deleteCase(int caseId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM cases WHERE id = ?")) {
            stmt.setInt(1, caseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete case: " + e.getMessage());
            return;
        }
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.CaseUpdatedEvent(caseId, "DELETED", null));
    }

    public int getActiveCaseCountByType(String playerUuid, String type) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM cases WHERE player_uuid = ? AND type = ? AND active = 1")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, type);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get active case count: " + e.getMessage());
        }
        return 0;
    }

    // ── Staff Notes (v1.5.0) ────────────────────────────────

    public void addNote(String playerName, String playerUuid, String note, String addedBy) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO staff_notes (player_name, player_uuid, note, added_by) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, playerName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, note);
            stmt.setString(4, addedBy);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add note: " + e.getMessage());
        }
    }

    public void removeNote(int noteId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM staff_notes WHERE id = ?")) {
            stmt.setInt(1, noteId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove note: " + e.getMessage());
        }
    }

    public List<NoteInfo> getNotesByPlayer(String playerName) {
        List<NoteInfo> notes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM staff_notes WHERE LOWER(player_name) = LOWER(?) ORDER BY added_at DESC")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(new NoteInfo(
                            rs.getInt("id"),
                            rs.getString("player_name"),
                            rs.getString("player_uuid"),
                            rs.getString("note"),
                            rs.getString("added_by"),
                            rs.getString("added_at")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get notes: " + e.getMessage());
        }
        return notes;
    }

    public List<NoteInfo> getNotesByUuid(String playerUuid) {
        List<NoteInfo> notes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM staff_notes WHERE player_uuid = ? ORDER BY added_at DESC")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(new NoteInfo(
                            rs.getInt("id"),
                            rs.getString("player_name"),
                            rs.getString("player_uuid"),
                            rs.getString("note"),
                            rs.getString("added_by"),
                            rs.getString("added_at")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get notes by UUID: " + e.getMessage());
        }
        return notes;
    }

    public boolean hasNotes(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM staff_notes WHERE player_uuid = ? LIMIT 1")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check notes: " + e.getMessage());
        }
        return false;
    }

    // ── Audit Log (v1.5.0) ──────────────────────────────────

    public void logAction(String staffName, String staffUuid, String action, String target, String details) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO audit_log (staff_name, staff_uuid, action, target, details) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, staffName);
            stmt.setString(2, staffUuid);
            stmt.setString(3, action);
            stmt.setString(4, target);
            stmt.setString(5, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to log action: " + e.getMessage());
        }
    }

    public List<AuditEntry> getAuditLogByStaff(String staffName, int limit) {
        List<AuditEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM audit_log WHERE LOWER(staff_name) = LOWER(?) ORDER BY timestamp DESC LIMIT ?")) {
            stmt.setString(1, staffName);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new AuditEntry(
                            rs.getInt("id"),
                            rs.getString("staff_name"),
                            rs.getString("staff_uuid"),
                            rs.getString("action"),
                            rs.getString("target"),
                            rs.getString("details"),
                            rs.getString("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get audit log: " + e.getMessage());
        }
        return entries;
    }

    public List<AuditEntry> getAuditLogByStaffAndDate(String staffName, String dateFrom, String dateTo, int limit) {
        List<AuditEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM audit_log WHERE LOWER(staff_name) = LOWER(?) AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC LIMIT ?")) {
            stmt.setString(1, staffName);
            stmt.setString(2, dateFrom);
            stmt.setString(3, dateTo);
            stmt.setInt(4, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new AuditEntry(
                            rs.getInt("id"),
                            rs.getString("staff_name"),
                            rs.getString("staff_uuid"),
                            rs.getString("action"),
                            rs.getString("target"),
                            rs.getString("details"),
                            rs.getString("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get filtered audit log: " + e.getMessage());
        }
        return entries;
    }

    // ── Reports (v1.6.0) ────────────────────────────────────

    public int createReport(String targetName, String targetUuid, String reason,
                            String reporterName, String reporterUuid, String chatLog) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO reports (target_name, target_uuid, reason, reporter_name, reporter_uuid, chat_log) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, targetName);
            stmt.setString(2, targetUuid);
            stmt.setString(3, reason);
            stmt.setString(4, reporterName);
            stmt.setString(5, reporterUuid);
            stmt.setString(6, chatLog);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create report: " + e.getMessage());
        }
        return -1;
    }

    public ReportInfo getReport(int reportId) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM reports WHERE id = ?")) {
            stmt.setInt(1, reportId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ReportInfo(
                            rs.getInt("id"), rs.getString("target_name"), rs.getString("target_uuid"),
                            rs.getString("reason"), rs.getString("reporter_name"), rs.getString("reporter_uuid"),
                            rs.getString("reported_at"), rs.getString("status"), rs.getString("claimed_by"),
                            rs.getString("closed_by"),
                            rs.getObject("linked_case_id") != null ? rs.getInt("linked_case_id") : null,
                            rs.getString("chat_log"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get report: " + e.getMessage());
        }
        return null;
    }

    public List<ReportInfo> getOpenReports() {
        List<ReportInfo> reports = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM reports WHERE status = 'OPEN' ORDER BY reported_at DESC")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(new ReportInfo(
                            rs.getInt("id"), rs.getString("target_name"), rs.getString("target_uuid"),
                            rs.getString("reason"), rs.getString("reporter_name"), rs.getString("reporter_uuid"),
                            rs.getString("reported_at"), rs.getString("status"), rs.getString("claimed_by"),
                            rs.getString("closed_by"),
                            rs.getObject("linked_case_id") != null ? rs.getInt("linked_case_id") : null,
                            rs.getString("chat_log")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get open reports: " + e.getMessage());
        }
        return reports;
    }

    public void claimReport(int reportId, String staffName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE reports SET status = 'CLAIMED', claimed_by = ? WHERE id = ?")) {
            stmt.setString(1, staffName);
            stmt.setInt(2, reportId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to claim report: " + e.getMessage());
            return;
        }
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.ReportUpdatedEvent(reportId, "CLAIMED", staffName, 0));
    }

    public void closeReport(int reportId, String staffName, Integer linkedCaseId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE reports SET status = 'CLOSED', closed_by = ?, linked_case_id = ? WHERE id = ?")) {
            stmt.setString(1, staffName);
            if (linkedCaseId != null) stmt.setInt(2, linkedCaseId);
            else stmt.setNull(2, java.sql.Types.INTEGER);
            stmt.setInt(3, reportId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close report: " + e.getMessage());
            return;
        }
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.ReportUpdatedEvent(reportId, "CLOSED", staffName,
                linkedCaseId != null ? linkedCaseId : 0));
    }

    // ── Staff Sessions (v1.6.0) ─────────────────────────────

    public void startDutySession(String staffUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO staff_sessions (staff_uuid) VALUES (?)")) {
            stmt.setString(1, staffUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to start duty session: " + e.getMessage());
        }
    }

    public void endDutySession(String staffUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE staff_sessions SET ended_at = CURRENT_TIMESTAMP WHERE staff_uuid = ? AND ended_at IS NULL")) {
            stmt.setString(1, staffUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to end duty session: " + e.getMessage());
        }
    }

    public long getStaffDutyMinutes(String staffUuid, String period) {
        String condition;
        switch (period) {
            case "day": condition = "AND started_at >= datetime('now', '-1 day')"; break;
            case "week": condition = "AND started_at >= datetime('now', '-7 days')"; break;
            default: condition = ""; break;
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT SUM((julianday(COALESCE(ended_at, CURRENT_TIMESTAMP)) - julianday(started_at)) * 1440) as minutes " +
                "FROM staff_sessions WHERE staff_uuid = ? " + condition)) {
            stmt.setString(1, staffUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return (long) rs.getDouble("minutes");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get duty time: " + e.getMessage());
        }
        return 0;
    }

    // ── Staff Stats (v1.6.0) ────────────────────────────────

    public int getStaffCaseCount(String staffName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM cases WHERE LOWER(issued_by) = LOWER(?)")) {
            stmt.setString(1, staffName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get staff case count: " + e.getMessage());
        }
        return 0;
    }

    public int getStaffCaseCountByType(String staffName, String type) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM cases WHERE LOWER(issued_by) = LOWER(?) AND type = ?")) {
            stmt.setString(1, staffName);
            stmt.setString(2, type);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get staff case count by type: " + e.getMessage());
        }
        return 0;
    }

    public int getStaffReportsClosed(String staffName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM reports WHERE LOWER(closed_by) = LOWER(?)")) {
            stmt.setString(1, staffName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get staff reports closed: " + e.getMessage());
        }
        return 0;
    }

    public String getUuidByName(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid FROM cases WHERE LOWER(player_name) = LOWER(?) AND player_uuid IS NOT NULL LIMIT 1")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("player_uuid");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get UUID by name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a player's UUID by their (latest) name: name history first (every player who joined
     * since v1.7.0), then cases. Returns null for players who never joined.
     */
    public String findUuidByName(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid FROM name_history WHERE LOWER(name) = LOWER(?) ORDER BY changed_at DESC LIMIT 1")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to find UUID by name: " + e.getMessage());
        }
        return getUuidByName(playerName);
    }

    public List<LeaderboardEntry> getStaffLeaderboard(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT issued_by, COUNT(*) as total FROM cases GROUP BY LOWER(issued_by) ORDER BY total DESC LIMIT ?")) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("issued_by");
                    int total = rs.getInt("total");
                    entries.add(new LeaderboardEntry(name, total));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get staff leaderboard: " + e.getMessage());
        }
        return entries;
    }

    // ══════════════════════════════════════════════════════════
    //                v1.7.0 – Player Protection
    // ══════════════════════════════════════════════════════════

    // ── Player Sessions ──────────────────────────────────────

    public int startPlayerSession(String playerUuid, String ip) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO player_sessions (player_uuid, ip, join_time) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, ip);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to start player session: " + e.getMessage());
        }
        return -1;
    }

    public void endPlayerSession(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE player_sessions SET quit_time = ? WHERE player_uuid = ? AND quit_time IS NULL")) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, playerUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to end player session: " + e.getMessage());
        }
    }

    public void closeOpenSessions() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE player_sessions SET quit_time = join_time WHERE quit_time IS NULL")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close open sessions: " + e.getMessage());
        }
        // v1.7.0 migration: mark instant punishment types as resolved
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE cases SET active = 0 WHERE type IN ('KICK', 'WARN') AND active = 1")) {
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                plugin.logInfo("Migrated " + updated + " KICK/WARN cases to resolved.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to migrate instant cases: " + e.getMessage());
        }
    }

    /** Playtime without AFK time (v1.9.0), used for trust progression and statistics. */
    public long getPlaytimeMinutes(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT SUM(COALESCE(quit_time, ?) - join_time) - SUM(COALESCE(afk_seconds, 0)) * 1000 as total "
                        + "FROM player_sessions WHERE player_uuid = ?")) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("total") / 60000;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get playtime: " + e.getMessage());
        }
        return 0;
    }

    public List<SessionInfo> getPlayerSessions(String playerUuid, int limit) {
        List<SessionInfo> sessions = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM player_sessions WHERE player_uuid = ? ORDER BY join_time DESC LIMIT ?")) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(new SessionInfo(
                            rs.getInt("id"), rs.getString("player_uuid"), rs.getString("ip"),
                            rs.getLong("join_time"), rs.getLong("quit_time")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player sessions: " + e.getMessage());
        }
        return sessions;
    }

    // ── IP History ───────────────────────────────────────────

    public void trackIp(String playerUuid, String ip) {
        long now = System.currentTimeMillis();
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT id FROM ip_history WHERE player_uuid = ? AND ip = ?")) {
            check.setString(1, playerUuid);
            check.setString(2, ip);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE ip_history SET last_seen = ? WHERE id = ?")) {
                        update.setLong(1, now);
                        update.setInt(2, rs.getInt("id"));
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO ip_history (player_uuid, ip, first_seen, last_seen) VALUES (?, ?, ?, ?)")) {
                        insert.setString(1, playerUuid);
                        insert.setString(2, ip);
                        insert.setLong(3, now);
                        insert.setLong(4, now);
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to track IP: " + e.getMessage());
        }
    }

    public List<IpHistoryEntry> getIpHistory(String playerUuid) {
        List<IpHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM ip_history WHERE player_uuid = ? ORDER BY last_seen DESC")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new IpHistoryEntry(
                            rs.getString("player_uuid"), rs.getString("ip"),
                            rs.getLong("first_seen"), rs.getLong("last_seen")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get IP history: " + e.getMessage());
        }
        return entries;
    }

    public List<String> getUuidsByIp(String ip) {
        List<String> uuids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT player_uuid FROM ip_history WHERE ip = ?")) {
            stmt.setString(1, ip);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) uuids.add(rs.getString("player_uuid"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get UUIDs by IP: " + e.getMessage());
        }
        return uuids;
    }

    // ── Name History ─────────────────────────────────────────

    public void trackName(String playerUuid, String name) {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT name FROM name_history WHERE player_uuid = ? ORDER BY changed_at DESC LIMIT 1")) {
            check.setString(1, playerUuid);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getString("name").equals(name)) return;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check name history: " + e.getMessage());
            return;
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO name_history (player_uuid, name, changed_at) VALUES (?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, name);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to track name: " + e.getMessage());
        }
    }

    public List<NameHistoryEntry> getNameHistory(String playerUuid) {
        List<NameHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM name_history WHERE player_uuid = ? ORDER BY changed_at DESC")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new NameHistoryEntry(
                            rs.getString("player_uuid"), rs.getString("name"), rs.getLong("changed_at")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get name history: " + e.getMessage());
        }
        return entries;
    }

    public String getLastKnownName(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM name_history WHERE player_uuid = ? ORDER BY changed_at DESC LIMIT 1")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get last known name: " + e.getMessage());
        }
        return null;
    }

    // ── Deaths ───────────────────────────────────────────────

    public int recordDeath(String playerUuid, String cause, String world, double x, double y, double z, int snapshotId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO player_deaths (player_uuid, cause, world, x, y, z, time, snapshot_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, cause);
            stmt.setString(3, world);
            stmt.setDouble(4, x);
            stmt.setDouble(5, y);
            stmt.setDouble(6, z);
            stmt.setLong(7, System.currentTimeMillis());
            stmt.setInt(8, snapshotId);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to record death: " + e.getMessage());
        }
        return -1;
    }

    public List<DeathInfo> getDeaths(String playerUuid, int limit) {
        List<DeathInfo> deaths = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM player_deaths WHERE player_uuid = ? ORDER BY time DESC LIMIT ?")) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    deaths.add(new DeathInfo(
                            rs.getInt("id"), rs.getString("player_uuid"), rs.getString("cause"),
                            rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getLong("time"), rs.getInt("snapshot_id")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get deaths: " + e.getMessage());
        }
        return deaths;
    }

    // ── Inventory Snapshots ──────────────────────────────────

    public int createSnapshot(String playerUuid, String playerName, String inventoryData,
                             String armorData, String offhandData, String enderchestData,
                             int xpLevel, float xpProgress, double health, int food,
                             String gamemode, String world, double x, double y, double z,
                             String reason, String deathCause) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO inventory_snapshots (player_uuid, player_name, inventory_data, armor_data, " +
                "offhand_data, enderchest_data, xp_level, xp_progress, health, food, gamemode, world, " +
                "x, y, z, reason, time, death_cause) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, playerName);
            stmt.setString(3, inventoryData);
            stmt.setString(4, armorData);
            stmt.setString(5, offhandData);
            stmt.setString(6, enderchestData);
            stmt.setInt(7, xpLevel);
            stmt.setFloat(8, xpProgress);
            stmt.setDouble(9, health);
            stmt.setInt(10, food);
            stmt.setString(11, gamemode);
            stmt.setString(12, world);
            stmt.setDouble(13, x);
            stmt.setDouble(14, y);
            stmt.setDouble(15, z);
            stmt.setString(16, reason);
            stmt.setLong(17, System.currentTimeMillis());
            stmt.setString(18, deathCause);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create snapshot: " + e.getMessage());
        }
        return -1;
    }

    public SnapshotInfo getSnapshot(int snapshotId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM inventory_snapshots WHERE id = ?")) {
            stmt.setInt(1, snapshotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readSnapshot(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get snapshot: " + e.getMessage());
        }
        return null;
    }

    public List<SnapshotInfo> getSnapshots(String playerUuid, int limit) {
        List<SnapshotInfo> snapshots = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM inventory_snapshots WHERE player_uuid = ? ORDER BY time DESC LIMIT ?")) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) snapshots.add(readSnapshot(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get snapshots: " + e.getMessage());
        }
        return snapshots;
    }

    private SnapshotInfo readSnapshot(ResultSet rs) throws SQLException {
        return new SnapshotInfo(
                rs.getInt("id"), rs.getString("player_uuid"), rs.getString("player_name"),
                rs.getString("inventory_data"), rs.getString("armor_data"),
                rs.getString("offhand_data"), rs.getString("enderchest_data"),
                rs.getInt("xp_level"), rs.getFloat("xp_progress"),
                rs.getDouble("health"), rs.getInt("food"), rs.getString("gamemode"),
                rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getString("reason"), rs.getLong("time"), rs.getString("death_cause"));
    }

    public void cleanupSnapshots(String playerUuid, int maxPerPlayer, int maxAgeDays) {
        long cutoff = System.currentTimeMillis() - (maxAgeDays * 86400000L);
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM inventory_snapshots WHERE player_uuid = ? AND time < ?")) {
            stmt.setString(1, playerUuid);
            stmt.setLong(2, cutoff);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to cleanup old snapshots: " + e.getMessage());
        }
        try (PreparedStatement count = connection.prepareStatement(
                "SELECT COUNT(*) FROM inventory_snapshots WHERE player_uuid = ?")) {
            count.setString(1, playerUuid);
            try (ResultSet rs = count.executeQuery()) {
                if (rs.next() && rs.getInt(1) > maxPerPlayer) {
                    int excess = rs.getInt(1) - maxPerPlayer;
                    try (PreparedStatement del = connection.prepareStatement(
                            "DELETE FROM inventory_snapshots WHERE id IN " +
                            "(SELECT id FROM inventory_snapshots WHERE player_uuid = ? ORDER BY time ASC LIMIT ?)")) {
                        del.setString(1, playerUuid);
                        del.setInt(2, excess);
                        del.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to cleanup excess snapshots: " + e.getMessage());
        }
    }

    public int getSnapshotItemCount(int snapshotId) {
        // approximate: count non-null inventory entries
        SnapshotInfo snap = getSnapshot(snapshotId);
        if (snap == null || snap.inventoryData == null) return 0;
        // Count items by counting separators in Base64 - rough estimate
        return snap.inventoryData.isEmpty() ? 0 : -1; // calculated in SnapshotManager
    }

    // ── Pending Restores ─────────────────────────────────────

    public void addPendingRestore(String playerUuid, int snapshotId, String type, String restoredBy) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO pending_restores (player_uuid, snapshot_id, type, restored_by) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, snapshotId);
            stmt.setString(3, type);
            stmt.setString(4, restoredBy);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add pending restore: " + e.getMessage());
        }
    }

    public List<PendingRestore> getPendingRestores(String playerUuid) {
        List<PendingRestore> restores = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM pending_restores WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    restores.add(new PendingRestore(
                            rs.getInt("id"), rs.getString("player_uuid"),
                            rs.getInt("snapshot_id"), rs.getString("type"), rs.getString("restored_by")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get pending restores: " + e.getMessage());
        }
        return restores;
    }

    public void removePendingRestore(int id) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM pending_restores WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove pending restore: " + e.getMessage());
        }
    }

    // ── Trust ────────────────────────────────────────────────

    public TrustInfo getTrust(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM player_trust WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new TrustInfo(
                            rs.getString("player_uuid"), rs.getString("level"),
                            rs.getInt("manual") == 1, rs.getLong("updated_at"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get trust: " + e.getMessage());
        }
        return null;
    }

    public void setTrust(String playerUuid, String level, boolean manual) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO player_trust (player_uuid, level, manual, updated_at) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, level);
            stmt.setInt(3, manual ? 1 : 0);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set trust: " + e.getMessage());
        }
    }

    public void addTrustHistory(String playerUuid, String oldLevel, String newLevel, String reason, String changedBy) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO trust_history (player_uuid, old_level, new_level, reason, changed_by, time) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, oldLevel);
            stmt.setString(3, newLevel);
            stmt.setString(4, reason);
            stmt.setString(5, changedBy);
            stmt.setLong(6, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add trust history: " + e.getMessage());
        }
    }

    public List<TrustHistoryEntry> getTrustHistory(String playerUuid, int limit) {
        List<TrustHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM trust_history WHERE player_uuid = ? ORDER BY time DESC LIMIT ?")) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new TrustHistoryEntry(
                            rs.getInt("id"), rs.getString("player_uuid"),
                            rs.getString("old_level"), rs.getString("new_level"),
                            rs.getString("reason"), rs.getString("changed_by"), rs.getLong("time")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get trust history: " + e.getMessage());
        }
        return entries;
    }

    public boolean hasRecentCase(String playerUuid, int days) {
        long cutoff = System.currentTimeMillis() - (days * 86400000L);
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM cases WHERE player_uuid = ? AND issued_at > ? LIMIT 1")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, String.valueOf(cutoff));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check recent cases: " + e.getMessage());
        }
        return false;
    }

    // ── VPN Cache ────────────────────────────────────────────

    public VpnCacheEntry getVpnCache(String ip) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM vpn_cache WHERE ip = ? AND expires_at > ?")) {
            stmt.setString(1, ip);
            stmt.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new VpnCacheEntry(
                            rs.getString("ip"), rs.getInt("is_vpn") == 1, rs.getLong("expires_at"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get VPN cache: " + e.getMessage());
        }
        return null;
    }

    public void setVpnCache(String ip, boolean isVpn, long expiresAt) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO vpn_cache (ip, is_vpn, expires_at) VALUES (?, ?, ?)")) {
            stmt.setString(1, ip);
            stmt.setInt(2, isVpn ? 1 : 0);
            stmt.setLong(3, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set VPN cache: " + e.getMessage());
        }
    }

    // ── Frozen Players ───────────────────────────────────────

    public void freezePlayer(String playerUuid, String frozenBy) {
        freezePlayer(playerUuid, frozenBy, 0L);
    }

    /** @param expiresAt epoch millis when the freeze ends, or 0 for permanent */
    public void freezePlayer(String playerUuid, String frozenBy, long expiresAt) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO frozen_players (player_uuid, frozen_by, frozen_at, expires_at) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, frozenBy);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setLong(4, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to freeze player: " + e.getMessage());
        }
    }

    public void unfreezePlayer(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM frozen_players WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unfreeze player: " + e.getMessage());
        }
    }

    public boolean isPlayerFrozen(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM frozen_players WHERE player_uuid = ? AND (expires_at IS NULL OR expires_at = 0 OR expires_at > ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check frozen: " + e.getMessage());
        }
        return false;
    }

    public List<String> getAllFrozenUuids() {
        List<String> uuids = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_uuid FROM frozen_players")) {
            while (rs.next()) uuids.add(rs.getString("player_uuid"));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get frozen players: " + e.getMessage());
        }
        return uuids;
    }

    /** Returns every frozen player's UUID mapped to its expiry (epoch millis, 0 = permanent). */
    public Map<String, Long> getFrozenPlayers() {
        Map<String, Long> frozen = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_uuid, expires_at FROM frozen_players")) {
            while (rs.next()) frozen.put(rs.getString("player_uuid"), rs.getLong("expires_at"));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get frozen players: " + e.getMessage());
        }
        return frozen;
    }

    // ── Homes ────────────────────────────────────────────────

    public void setHome(String playerUuid, String name, String world, double x, double y, double z, float yaw, float pitch) {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT id FROM homes WHERE player_uuid = ? AND LOWER(name) = LOWER(?)")) {
            check.setString(1, playerUuid);
            check.setString(2, name);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE homes SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ? WHERE id = ?")) {
                        update.setString(1, world);
                        update.setDouble(2, x);
                        update.setDouble(3, y);
                        update.setDouble(4, z);
                        update.setFloat(5, yaw);
                        update.setFloat(6, pitch);
                        update.setInt(7, rs.getInt("id"));
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO homes (player_uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        insert.setString(1, playerUuid);
                        insert.setString(2, name);
                        insert.setString(3, world);
                        insert.setDouble(4, x);
                        insert.setDouble(5, y);
                        insert.setDouble(6, z);
                        insert.setFloat(7, yaw);
                        insert.setFloat(8, pitch);
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set home: " + e.getMessage());
        }
    }

    public HomeInfo getHome(String playerUuid, String name) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM homes WHERE player_uuid = ? AND LOWER(name) = LOWER(?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readHome(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get home: " + e.getMessage());
        }
        return null;
    }

    public List<HomeInfo> getHomes(String playerUuid) {
        List<HomeInfo> homes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM homes WHERE player_uuid = ? ORDER BY name ASC")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) homes.add(readHome(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get homes: " + e.getMessage());
        }
        return homes;
    }

    private HomeInfo readHome(ResultSet rs) throws SQLException {
        return new HomeInfo(
                rs.getInt("id"), rs.getString("player_uuid"), rs.getString("name"),
                rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getFloat("yaw"), rs.getFloat("pitch"));
    }

    public void deleteHome(String playerUuid, String name) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM homes WHERE player_uuid = ? AND LOWER(name) = LOWER(?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete home: " + e.getMessage());
        }
    }

    public int getHomeCount(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM homes WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get home count: " + e.getMessage());
        }
        return 0;
    }

    public List<String> getHomeNames(String playerUuid) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM homes WHERE player_uuid = ? ORDER BY name ASC")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get home names: " + e.getMessage());
        }
        return names;
    }

    // ── Alts (computed from ip_history) ──────────────────────

    public List<String> getAltUuids(String playerUuid) {
        List<String> altUuids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT ih2.player_uuid FROM ip_history ih1 " +
                "JOIN ip_history ih2 ON ih1.ip = ih2.ip " +
                "WHERE ih1.player_uuid = ? AND ih2.player_uuid != ?")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) altUuids.add(rs.getString("player_uuid"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get alt UUIDs: " + e.getMessage());
        }
        return altUuids;
    }

    /**
     * Adds AFK time to the player's latest session (v1.9.0). Targets the newest session by ID, so
     * it works whether or not the session was already closed on quit.
     */
    public void addAfkSeconds(String playerUuid, long seconds) {
        if (seconds <= 0) return;
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE player_sessions SET afk_seconds = COALESCE(afk_seconds, 0) + ? WHERE id = "
                        + "(SELECT id FROM player_sessions WHERE player_uuid = ? ORDER BY join_time DESC LIMIT 1)")) {
            stmt.setLong(1, seconds);
            stmt.setString(2, playerUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save AFK time: " + e.getMessage());
        }
    }

    /** Start of the player's open session, or 0. */
    public long getCurrentSessionStart(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT MAX(join_time) FROM player_sessions WHERE player_uuid = ? AND quit_time IS NULL")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get session start: " + e.getMessage());
        }
        return 0;
    }

    /** First recorded session start (sessions exist since v1.7.0), or 0. */
    public long getFirstSessionTime(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT MIN(join_time) FROM player_sessions WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get first session time: " + e.getMessage());
        }
        return 0;
    }

    public long getLastSessionTime(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT MAX(COALESCE(quit_time, join_time)) FROM player_sessions WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get last session time: " + e.getMessage());
        }
        return 0;
    }

    public int getRecentReportCount(String playerUuid, int minutes) {
        long cutoff = System.currentTimeMillis() - (minutes * 60000L);
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM reports WHERE target_uuid = ? AND reported_at > ?")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, String.valueOf(cutoff));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get recent report count: " + e.getMessage());
        }
        return 0;
    }

    // ══════════════════════════════════════════════════════════
    //                v1.8.0 – Server Health
    // ══════════════════════════════════════════════════════════

    // ── TPS History ─────────────────────────────────────────

    public void insertTpsHistory(long time, double tps1m, double tps5m, double tps15m,
                                 double msptAvg, double msptMax, long memoryUsed, long memoryMax,
                                 int healthScore, int onlinePlayers) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO tps_history (time, tps_1m, tps_5m, tps_15m, mspt_avg, mspt_max, " +
                "memory_used, memory_max, health_score, online_players) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setLong(1, time);
            stmt.setDouble(2, tps1m);
            stmt.setDouble(3, tps5m);
            stmt.setDouble(4, tps15m);
            stmt.setDouble(5, msptAvg);
            stmt.setDouble(6, msptMax);
            stmt.setLong(7, memoryUsed);
            stmt.setLong(8, memoryMax);
            stmt.setInt(9, healthScore);
            stmt.setInt(10, onlinePlayers);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert TPS history: " + e.getMessage());
        }
    }

    public List<TpsHistoryEntry> getTpsHistory(long since, int limit) {
        List<TpsHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM tps_history WHERE time >= ? ORDER BY time DESC LIMIT ?")) {
            stmt.setLong(1, since);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new TpsHistoryEntry(
                            rs.getLong("time"), rs.getDouble("tps_1m"), rs.getDouble("tps_5m"),
                            rs.getDouble("tps_15m"), rs.getDouble("mspt_avg"), rs.getDouble("mspt_max"),
                            rs.getLong("memory_used"), rs.getLong("memory_max"),
                            rs.getInt("health_score"), rs.getInt("online_players")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get TPS history: " + e.getMessage());
        }
        return entries;
    }

    public TpsHistoryEntry getLatestTpsHistory() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM tps_history ORDER BY time DESC LIMIT 1")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new TpsHistoryEntry(
                            rs.getLong("time"), rs.getDouble("tps_1m"), rs.getDouble("tps_5m"),
                            rs.getDouble("tps_15m"), rs.getDouble("mspt_avg"), rs.getDouble("mspt_max"),
                            rs.getLong("memory_used"), rs.getLong("memory_max"),
                            rs.getInt("health_score"), rs.getInt("online_players"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get latest TPS history: " + e.getMessage());
        }
        return null;
    }

    public void purgeTpsHistory(int retentionDays) {
        long cutoff = System.currentTimeMillis() - (retentionDays * 86400000L);
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM tps_history WHERE time < ?")) {
            stmt.setLong(1, cutoff);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to purge TPS history: " + e.getMessage());
        }
    }

    // ── Lag Alerts ──────────────────────────────────────────

    public int insertLagAlert(long time, double tps, int durationSeconds, int healthScore) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO lag_alerts (time, tps, duration_seconds, health_score) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, time);
            stmt.setDouble(2, tps);
            stmt.setInt(3, durationSeconds);
            stmt.setInt(4, healthScore);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert lag alert: " + e.getMessage());
        }
        return -1;
    }

    public void addLagAlertChunk(int alertId, String world, int chunkX, int chunkZ,
                                 double score, int entityCount, int tileEntityCount,
                                 String topContributorUuid, String topContributorName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO lag_alert_chunks (alert_id, world, chunk_x, chunk_z, score, entity_count, " +
                "tile_entity_count, top_contributor_uuid, top_contributor_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, String.valueOf(alertId));
            stmt.setString(2, world);
            stmt.setInt(3, chunkX);
            stmt.setInt(4, chunkZ);
            stmt.setDouble(5, score);
            stmt.setInt(6, entityCount);
            stmt.setInt(7, tileEntityCount);
            stmt.setString(8, topContributorUuid);
            stmt.setString(9, topContributorName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add lag alert chunk: " + e.getMessage());
        }
    }

    public void resolveLagAlert(int alertId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE lag_alerts SET resolved = 1, resolved_at = ? WHERE id = ?")) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setInt(2, alertId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to resolve lag alert: " + e.getMessage());
        }
    }

    public LagAlertInfo getLatestUnresolvedAlert() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM lag_alerts WHERE resolved = 0 ORDER BY time DESC LIMIT 1")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readLagAlert(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get unresolved alert: " + e.getMessage());
        }
        return null;
    }

    public List<LagAlertInfo> getRecentAlerts(int limit) {
        List<LagAlertInfo> alerts = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM lag_alerts ORDER BY time DESC LIMIT ?")) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) alerts.add(readLagAlert(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get recent alerts: " + e.getMessage());
        }
        return alerts;
    }

    public List<LagAlertChunkInfo> getAlertChunks(int alertId) {
        List<LagAlertChunkInfo> chunks = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM lag_alert_chunks WHERE alert_id = ? ORDER BY score DESC")) {
            stmt.setInt(1, alertId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chunks.add(new LagAlertChunkInfo(
                            rs.getInt("id"), rs.getInt("alert_id"), rs.getString("world"),
                            rs.getInt("chunk_x"), rs.getInt("chunk_z"), rs.getDouble("score"),
                            rs.getInt("entity_count"), rs.getInt("tile_entity_count"),
                            rs.getString("top_contributor_uuid"), rs.getString("top_contributor_name")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get alert chunks: " + e.getMessage());
        }
        return chunks;
    }

    private LagAlertInfo readLagAlert(ResultSet rs) throws SQLException {
        return new LagAlertInfo(
                rs.getInt("id"), rs.getLong("time"), rs.getDouble("tps"),
                rs.getInt("duration_seconds"), rs.getInt("health_score"),
                rs.getInt("resolved") == 1, rs.getLong("resolved_at"));
    }

    // ── Chunk Activity ──────────────────────────────────────

    public void insertChunkActivity(String playerUuid, String playerName, String world,
                                    int chunkX, int chunkZ, String actionType, String detail, int count) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO chunk_activity (player_uuid, player_name, world, chunk_x, chunk_z, " +
                "action_type, detail, count, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, playerName);
            stmt.setString(3, world);
            stmt.setInt(4, chunkX);
            stmt.setInt(5, chunkZ);
            stmt.setString(6, actionType);
            stmt.setString(7, detail);
            stmt.setInt(8, count);
            stmt.setLong(9, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert chunk activity: " + e.getMessage());
        }
    }

    public void batchInsertChunkActivity(List<ChunkActivityEntry> entries) {
        String sql = "INSERT INTO chunk_activity (player_uuid, player_name, world, chunk_x, chunk_z, " +
                "action_type, detail, count, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (ChunkActivityEntry e : entries) {
                stmt.setString(1, e.playerUuid);
                stmt.setString(2, e.playerName);
                stmt.setString(3, e.world);
                stmt.setInt(4, e.chunkX);
                stmt.setInt(5, e.chunkZ);
                stmt.setString(6, e.actionType);
                stmt.setString(7, e.detail);
                stmt.setInt(8, e.count);
                stmt.setLong(9, e.time);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to batch insert chunk activity: " + e.getMessage());
        }
    }

    public List<ChunkActivityEntry> getChunkActivity(String world, int chunkX, int chunkZ, long since) {
        List<ChunkActivityEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM chunk_activity WHERE world = ? AND chunk_x = ? AND chunk_z = ? AND time >= ? ORDER BY time DESC")) {
            stmt.setString(1, world);
            stmt.setInt(2, chunkX);
            stmt.setInt(3, chunkZ);
            stmt.setLong(4, since);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) entries.add(readChunkActivity(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get chunk activity: " + e.getMessage());
        }
        return entries;
    }

    public List<ChunkActivityEntry> getPlayerChunkActivity(String playerUuid, long since) {
        List<ChunkActivityEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM chunk_activity WHERE player_uuid = ? AND time >= ? ORDER BY time DESC")) {
            stmt.setString(1, playerUuid);
            stmt.setLong(2, since);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) entries.add(readChunkActivity(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player chunk activity: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Gets the top contributors to a specific chunk, aggregated by total count.
     */
    public List<ChunkActivityEntry> getTopChunkContributors(String world, int chunkX, int chunkZ, long since, int limit) {
        List<ChunkActivityEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid, player_name, SUM(count) as total " +
                "FROM chunk_activity WHERE world = ? AND chunk_x = ? AND chunk_z = ? AND time >= ? " +
                "GROUP BY player_uuid ORDER BY total DESC LIMIT ?")) {
            stmt.setString(1, world);
            stmt.setInt(2, chunkX);
            stmt.setInt(3, chunkZ);
            stmt.setLong(4, since);
            stmt.setInt(5, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new ChunkActivityEntry(
                            0, rs.getString("player_uuid"), rs.getString("player_name"),
                            world, chunkX, chunkZ, "TOTAL", null, rs.getInt("total"), 0));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get top chunk contributors: " + e.getMessage());
        }
        return entries;
    }

    public void purgeChunkActivity(int retentionDays) {
        long cutoff = System.currentTimeMillis() - (retentionDays * 86400000L);
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM chunk_activity WHERE time < ?")) {
            stmt.setLong(1, cutoff);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to purge chunk activity: " + e.getMessage());
        }
    }

    private ChunkActivityEntry readChunkActivity(ResultSet rs) throws SQLException {
        return new ChunkActivityEntry(
                rs.getInt("id"), rs.getString("player_uuid"), rs.getString("player_name"),
                rs.getString("world"), rs.getInt("chunk_x"), rs.getInt("chunk_z"),
                rs.getString("action_type"), rs.getString("detail"),
                rs.getInt("count"), rs.getLong("time"));
    }

    // ── Warps ───────────────────────────────────────────────

    public void setWarp(String name, String world, double x, double y, double z,
                        float yaw, float pitch, String icon, String createdBy, String permission) {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT id FROM warps WHERE LOWER(name) = LOWER(?)")) {
            check.setString(1, name);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    // Moving an existing warp keeps its icon, permission and position in the list
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE warps SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?, " +
                            "created_by = ?, created_at = ? WHERE id = ?")) {
                        update.setString(1, world);
                        update.setDouble(2, x);
                        update.setDouble(3, y);
                        update.setDouble(4, z);
                        update.setFloat(5, yaw);
                        update.setFloat(6, pitch);
                        update.setString(7, createdBy);
                        update.setLong(8, System.currentTimeMillis());
                        update.setInt(9, rs.getInt("id"));
                        update.executeUpdate();
                    }
                } else {
                    // New warps go to the end of the list
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO warps (name, world, x, y, z, yaw, pitch, icon, created_by, created_at, permission, sort_order) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                            "(SELECT COALESCE(MAX(sort_order), 0) + 1 FROM warps))")) {
                        insert.setString(1, name);
                        insert.setString(2, world);
                        insert.setDouble(3, x);
                        insert.setDouble(4, y);
                        insert.setDouble(5, z);
                        insert.setFloat(6, yaw);
                        insert.setFloat(7, pitch);
                        insert.setString(8, icon);
                        insert.setString(9, createdBy);
                        insert.setLong(10, System.currentTimeMillis());
                        insert.setString(11, permission);
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set warp: " + e.getMessage());
        }
    }

    public WarpInfo getWarp(String name) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM warps WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readWarp(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warp: " + e.getMessage());
        }
        return null;
    }

    public List<WarpInfo> getAllWarps() {
        List<WarpInfo> warps = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM warps ORDER BY sort_order ASC, name ASC")) {
            while (rs.next()) warps.add(readWarp(rs));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get all warps: " + e.getMessage());
        }
        return warps;
    }

    public List<String> getWarpNames() {
        List<String> names = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM warps ORDER BY sort_order ASC, name ASC")) {
            while (rs.next()) names.add(rs.getString("name"));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warp names: " + e.getMessage());
        }
        return names;
    }

    public void deleteWarp(String name) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM warps WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete warp: " + e.getMessage());
        }
    }

    public void updateWarpIcon(String name, String icon) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE warps SET icon = ? WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, icon);
            stmt.setString(2, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update warp icon: " + e.getMessage());
        }
    }

    public void updateWarpPermission(String name, String permission) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE warps SET permission = ? WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, permission);
            stmt.setString(2, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update warp permission: " + e.getMessage());
        }
    }

    /** Renames a warp. Callers must check that the new name is not already taken. */
    public boolean renameWarp(String oldName, String newName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE warps SET name = ? WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, newName);
            stmt.setString(2, oldName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to rename warp: " + e.getMessage());
            return false;
        }
    }

    /**
     * Moves a warp to the given 0-based position in the list and renumbers all warps.
     * The position is clamped to the list bounds.
     *
     * @return the warp's new 0-based position, or -1 if the warp does not exist
     */
    public synchronized int moveWarp(String name, int position) {
        List<String> names = getWarpNames();
        String match = null;
        for (String n : names) {
            if (n.equalsIgnoreCase(name)) {
                match = n;
                break;
            }
        }
        if (match == null) return -1;

        names.remove(match);
        int target = Math.max(0, Math.min(position, names.size()));
        names.add(target, match);

        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE warps SET sort_order = ? WHERE name = ?")) {
            for (int i = 0; i < names.size(); i++) {
                stmt.setInt(1, i + 1);
                stmt.setString(2, names.get(i));
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to reorder warps: " + e.getMessage());
            return -1;
        }
        return target;
    }

    private WarpInfo readWarp(ResultSet rs) throws SQLException {
        return new WarpInfo(
                rs.getInt("id"), rs.getString("name"), rs.getString("world"),
                rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getFloat("yaw"), rs.getFloat("pitch"), rs.getString("icon"),
                rs.getString("created_by"), rs.getLong("created_at"), rs.getString("permission"));
    }

    // ── RTP Cooldowns ───────────────────────────────────────

    public void setRtpCooldown(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO rtp_cooldowns (player_uuid, last_used) VALUES (?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set RTP cooldown: " + e.getMessage());
        }
    }

    public long getRtpCooldown(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT last_used FROM rtp_cooldowns WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("last_used");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get RTP cooldown: " + e.getMessage());
        }
        return 0;
    }

    // ── Maintenance ─────────────────────────────────────────

    public void setMaintenance(boolean active, String activatedBy, int durationMinutes, String reason) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO maintenance (id, active, activated_by, activated_at, duration_minutes, reason) " +
                "VALUES (1, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, active ? 1 : 0);
            stmt.setString(2, activatedBy);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setInt(4, durationMinutes);
            stmt.setString(5, reason);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set maintenance: " + e.getMessage());
        }
    }

    public MaintenanceInfo getMaintenance() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM maintenance WHERE id = 1")) {
            if (rs.next()) {
                return new MaintenanceInfo(
                        rs.getInt("active") == 1, rs.getString("activated_by"),
                        rs.getLong("activated_at"), rs.getInt("duration_minutes"),
                        rs.getString("reason"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get maintenance: " + e.getMessage());
        }
        return null;
    }

    public void clearMaintenance() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE maintenance SET active = 0 WHERE id = 1")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clear maintenance: " + e.getMessage());
        }
    }

    // ── Discord links (v1.9.0) ──────────────────────────────

    public static class DiscordLink {
        public final String playerUuid;
        public final String playerName;
        public final String discordId;
        public final String discordName;
        public final long linkedAt;
        public final String permSnapshot;

        public DiscordLink(String playerUuid, String playerName, String discordId, String discordName,
                           long linkedAt, String permSnapshot) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.discordId = discordId;
            this.discordName = discordName;
            this.linkedAt = linkedAt;
            this.permSnapshot = permSnapshot;
        }

        /** Whether the player had this permission on their last join (for offline staff checks). */
        public boolean hadPermission(String permission) {
            if (permSnapshot == null || permSnapshot.isEmpty()) return false;
            for (String p : permSnapshot.split(",")) {
                if (p.equalsIgnoreCase(permission)) return true;
            }
            return false;
        }
    }

    private DiscordLink readLink(ResultSet rs) throws SQLException {
        return new DiscordLink(rs.getString("player_uuid"), rs.getString("player_name"), rs.getString("discord_id"),
                rs.getString("discord_name"), rs.getLong("linked_at"), rs.getString("perm_snapshot"));
    }

    public DiscordLink getLinkByUuid(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM discord_links WHERE player_uuid = ? ORDER BY linked_at DESC LIMIT 1")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readLink(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get Discord link: " + e.getMessage());
        }
        return null;
    }

    public DiscordLink getLinkByDiscord(String discordId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM discord_links WHERE discord_id = ? ORDER BY linked_at DESC LIMIT 1")) {
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readLink(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get Discord link: " + e.getMessage());
        }
        return null;
    }

    public List<DiscordLink> getAllLinks() {
        List<DiscordLink> links = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM discord_links");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) links.add(readLink(rs));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get Discord links: " + e.getMessage());
        }
        return links;
    }

    public void addLink(String playerUuid, String playerName, String discordId, String discordName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO discord_links (player_uuid, player_name, discord_id, discord_name, linked_at) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, playerName);
            stmt.setString(3, discordId);
            stmt.setString(4, discordName);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save Discord link: " + e.getMessage());
        }
    }

    /** Removes all links of a player. Returns the removed links. */
    public List<DiscordLink> removeLinksByUuid(String playerUuid) {
        return removeLinks("player_uuid", playerUuid);
    }

    /** Removes all links of a Discord account. Returns the removed links. */
    public List<DiscordLink> removeLinksByDiscord(String discordId) {
        return removeLinks("discord_id", discordId);
    }

    private List<DiscordLink> removeLinks(String column, String value) {
        List<DiscordLink> removed = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement("SELECT * FROM discord_links WHERE " + column + " = ?");
             PreparedStatement delete = connection.prepareStatement("DELETE FROM discord_links WHERE " + column + " = ?")) {
            select.setString(1, value);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) removed.add(readLink(rs));
            }
            delete.setString(1, value);
            delete.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove Discord link: " + e.getMessage());
        }
        return removed;
    }

    public void updateLinkProfile(String playerUuid, String playerName, String permSnapshot) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE discord_links SET player_name = ?, perm_snapshot = ? WHERE player_uuid = ?")) {
            stmt.setString(1, playerName);
            stmt.setString(2, permSnapshot);
            stmt.setString(3, playerUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update Discord link: " + e.getMessage());
        }
    }

    /** Creates a new 6-digit code for a player, replacing their previous one. */
    public String createLinkCode(String playerUuid, String playerName, int minutes) {
        long now = System.currentTimeMillis();
        try (PreparedStatement clean = connection.prepareStatement(
                "DELETE FROM link_codes WHERE player_uuid = ? OR expires_at < ?")) {
            clean.setString(1, playerUuid);
            clean.setLong(2, now);
            clean.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clean link codes: " + e.getMessage());
        }
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = String.format("%06d", random.nextInt(1_000_000));
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO link_codes (code, player_uuid, player_name, expires_at) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, code);
                stmt.setString(2, playerUuid);
                stmt.setString(3, playerName);
                stmt.setLong(4, now + minutes * 60_000L);
                stmt.executeUpdate();
                return code;
            } catch (SQLException ignored) {
                // Code already in use by someone else, try another
            }
        }
        return null;
    }

    /** Uses up a valid code. Returns {uuid, name}, or null when it's unknown or expired. */
    public String[] takeLinkCode(String code) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid, player_name, expires_at FROM link_codes WHERE code = ?")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                String[] result = { rs.getString(1), rs.getString(2) };
                boolean valid = rs.getLong(3) >= System.currentTimeMillis();
                try (PreparedStatement del = connection.prepareStatement("DELETE FROM link_codes WHERE code = ?")) {
                    del.setString(1, code);
                    del.executeUpdate();
                }
                return valid ? result : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to use link code: " + e.getMessage());
        }
        return null;
    }

    // ── Appeals (v1.9.0) ────────────────────────────────────

    public static class AppealInfo {
        public final int id;
        public final int caseId;
        public final String playerUuid;
        public final String playerName;
        public final String discordId;
        /** question \u001F answer, entries separated by \u001E */
        public final String answers;
        /** OPEN, ACCEPTED, DENIED or REDUCED */
        public final String status;
        public final String decidedBy;
        public final String decision;
        public final long createdAt;
        public final long decidedAt;

        public AppealInfo(int id, int caseId, String playerUuid, String playerName, String discordId, String answers,
                          String status, String decidedBy, String decision, long createdAt, long decidedAt) {
            this.id = id;
            this.caseId = caseId;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.discordId = discordId;
            this.answers = answers;
            this.status = status;
            this.decidedBy = decidedBy;
            this.decision = decision;
            this.createdAt = createdAt;
            this.decidedAt = decidedAt;
        }

        /** Question/answer pairs in order. */
        public List<String[]> answerPairs() {
            List<String[]> pairs = new ArrayList<>();
            if (answers == null || answers.isEmpty()) return pairs;
            for (String entry : answers.split("\u001E")) {
                String[] qa = entry.split("\u001F", 2);
                pairs.add(new String[]{ qa[0], qa.length > 1 ? qa[1] : "" });
            }
            return pairs;
        }
    }

    private AppealInfo readAppeal(ResultSet rs) throws SQLException {
        return new AppealInfo(rs.getInt("id"), rs.getInt("case_id"), rs.getString("player_uuid"),
                rs.getString("player_name"), rs.getString("discord_id"), rs.getString("answers"),
                rs.getString("status"), rs.getString("decided_by"), rs.getString("decision"),
                rs.getLong("created_at"), rs.getLong("decided_at"));
    }

    public int createAppeal(int caseId, String playerUuid, String playerName, String discordId, String answers) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO appeals (case_id, player_uuid, player_name, discord_id, answers, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, caseId);
            stmt.setString(2, playerUuid);
            stmt.setString(3, playerName);
            stmt.setString(4, discordId);
            stmt.setString(5, answers);
            stmt.setLong(6, System.currentTimeMillis());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create appeal: " + e.getMessage());
        }
        return -1;
    }

    public AppealInfo getAppeal(int appealId) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM appeals WHERE id = ?")) {
            stmt.setInt(1, appealId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readAppeal(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get appeal: " + e.getMessage());
        }
        return null;
    }

    /** The newest appeal for a case, or null. */
    public AppealInfo getLatestAppealForCase(int caseId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM appeals WHERE case_id = ? ORDER BY created_at DESC LIMIT 1")) {
            stmt.setInt(1, caseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return readAppeal(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get appeal: " + e.getMessage());
        }
        return null;
    }

    /** Appeals, newest first. onlyOpen = status OPEN only. */
    public List<AppealInfo> getAppeals(boolean onlyOpen, int limit) {
        List<AppealInfo> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM appeals" + (onlyOpen ? " WHERE status = 'OPEN'" : "") + " ORDER BY created_at DESC LIMIT ?")) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(readAppeal(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get appeals: " + e.getMessage());
        }
        return list;
    }

    /**
     * Marks an open appeal as decided. Returns false when it was already decided (two staff
     * clicking at the same time), so only one decision is ever applied.
     */
    public boolean decideAppeal(int appealId, String status, String decidedBy, String decision) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE appeals SET status = ?, decided_by = ?, decision = ?, decided_at = ? WHERE id = ? AND status = 'OPEN'")) {
            stmt.setString(1, status);
            stmt.setString(2, decidedBy);
            stmt.setString(3, decision);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setInt(5, appealId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to decide appeal: " + e.getMessage());
            return false;
        }
    }

    /** Counts appeals in a time range: {submitted, accepted (incl. reduced), denied}. */
    public int[] countAppeals(long from, long to) {
        int[] counts = new int[3];
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT status, COUNT(*) FROM appeals WHERE created_at >= ? AND created_at < ? GROUP BY status")) {
            stmt.setLong(1, from);
            stmt.setLong(2, to);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int n = rs.getInt(2);
                    counts[0] += n;
                    String status = rs.getString(1);
                    if ("ACCEPTED".equals(status) || "REDUCED".equals(status)) counts[1] += n;
                    else if ("DENIED".equals(status)) counts[2] += n;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to count appeals: " + e.getMessage());
        }
        return counts;
    }

    // ── Weekly report (v1.9.0) ──────────────────────────────

    /** Numbers for the weekly Discord report, all read from existing tables. */
    public static class WeeklyStats {
        public int newPlayers, uniquePlayers, peakPlayers, sessions, lagAlerts;
        public long peakTime, playtimeMillis;
        public double avgTps, avgHealth;
        public final Map<String, Integer> casesByType = new java.util.TreeMap<>();
        public final Map<String, Integer> casesByReason = new HashMap<>();
        /** staff name -> {cases, reports closed, duty minutes} */
        public final Map<String, long[]> staff = new HashMap<>();
        public int[] appeals = new int[3];
    }

    /** Collects the weekly report numbers for [from, to). Call off the main thread. */
    public WeeklyStats getWeeklyStats(long from, long to) {
        WeeklyStats s = new WeeklyStats();
        // cases.issued_at, staff_sessions and reports use CURRENT_TIMESTAMP (UTC text)
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(java.time.ZoneOffset.UTC);
        String fromText = fmt.format(java.time.Instant.ofEpochMilli(from));
        String toText = fmt.format(java.time.Instant.ofEpochMilli(to));

        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM (SELECT player_uuid, MIN(join_time) AS first FROM player_sessions GROUP BY player_uuid) "
                            + "WHERE first >= ? AND first < ?")) {
                stmt.setLong(1, from);
                stmt.setLong(2, to);
                try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) s.newPlayers = rs.getInt(1); }
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(DISTINCT player_uuid), COUNT(*), "
                            + "SUM(MIN(COALESCE(quit_time, ?), ?) - join_time - COALESCE(afk_seconds, 0) * 1000) "
                            + "FROM player_sessions WHERE join_time >= ? AND join_time < ?")) {
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setLong(2, to);
                stmt.setLong(3, from);
                stmt.setLong(4, to);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        s.uniquePlayers = rs.getInt(1);
                        s.sessions = rs.getInt(2);
                        s.playtimeMillis = Math.max(0, rs.getLong(3));
                    }
                }
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT online_players, time FROM tps_history WHERE time >= ? AND time < ? ORDER BY online_players DESC, time ASC LIMIT 1")) {
                stmt.setLong(1, from);
                stmt.setLong(2, to);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) { s.peakPlayers = rs.getInt(1); s.peakTime = rs.getLong(2); }
                }
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT AVG(tps_1m), AVG(health_score) FROM tps_history WHERE time >= ? AND time < ?")) {
                stmt.setLong(1, from);
                stmt.setLong(2, to);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) { s.avgTps = rs.getDouble(1); s.avgHealth = rs.getDouble(2); }
                }
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM lag_alerts WHERE time >= ? AND time < ?")) {
                stmt.setLong(1, from);
                stmt.setLong(2, to);
                try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) s.lagAlerts = rs.getInt(1); }
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT type, reason, issued_by FROM cases WHERE issued_at >= ? AND issued_at < ?")) {
                stmt.setString(1, fromText);
                stmt.setString(2, toText);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        s.casesByType.merge(rs.getString(1), 1, Integer::sum);
                        if (rs.getString(2) != null) s.casesByReason.merge(rs.getString(2), 1, Integer::sum);
                        String by = rs.getString(3);
                        if (by != null && !by.equalsIgnoreCase("CONSOLE")) {
                            s.staff.computeIfAbsent(by, k -> new long[3])[0]++;
                        }
                    }
                }
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT closed_by, COUNT(*) FROM reports WHERE status = 'CLOSED' AND closed_by IS NOT NULL "
                            + "AND reported_at >= ? AND reported_at < ? GROUP BY closed_by")) {
                stmt.setString(1, fromText);
                stmt.setString(2, toText);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) s.staff.computeIfAbsent(rs.getString(1), k -> new long[3])[1] += rs.getInt(2);
                }
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT staff_uuid, SUM((julianday(COALESCE(ended_at, CURRENT_TIMESTAMP)) - julianday(started_at)) * 1440) "
                            + "FROM staff_sessions WHERE started_at >= ? AND started_at < ? GROUP BY staff_uuid")) {
                stmt.setString(1, fromText);
                stmt.setString(2, toText);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = getLastKnownName(rs.getString(1));
                        if (name != null) s.staff.computeIfAbsent(name, k -> new long[3])[2] += Math.round(rs.getDouble(2));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to collect weekly stats: " + e.getMessage());
        }
        s.appeals = countAppeals(from, to);
        return s;
    }

    // ── Discord messages (v1.9.0) ───────────────────────────

    /** Remembers which Discord message shows a case, report or appeal (kind CASE/REPORT/APPEAL). */
    public void saveDiscordMessage(String kind, int refId, String channelId, String messageId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO discord_messages (kind, ref_id, channel_id, message_id) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, kind);
            stmt.setInt(2, refId);
            stmt.setString(3, channelId);
            stmt.setString(4, messageId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save Discord message: " + e.getMessage());
        }
    }

    /** Returns {channelId, messageId}, or null when this case/report/appeal wasn't posted. */
    public String[] getDiscordMessage(String kind, int refId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT channel_id, message_id FROM discord_messages WHERE kind = ? AND ref_id = ?")) {
            stmt.setString(1, kind);
            stmt.setInt(2, refId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return new String[]{ rs.getString(1), rs.getString(2) };
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get Discord message: " + e.getMessage());
        }
        return null;
    }

    public Connection getConnection() {
        return connection;
    }
}
