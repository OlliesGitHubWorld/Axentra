package mc.ollie;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final App plugin;
    private Connection connection;

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

        public CaseInfo(int id, String type, String playerName, String playerUuid,
                        String reason, String issuedBy, String issuedAt, String expiresAt,
                        boolean active, String chatLog) {
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

    // ── Setup ───────────────────────────────────────────────

    public void setup() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

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
            }

            plugin.getLogger().info("Database connected!");
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
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE bans SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1")) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
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
    }

    public void unmutePlayer(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE mutes SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1")) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unmute player: " + e.getMessage());
        }
    }

    public boolean isMuted(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM mutes WHERE player_uuid = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check mute: " + e.getMessage());
            return false;
        }
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

    public List<String> getActiveWarnedNames() {
        List<String> names = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT player_name FROM warns WHERE active = 1")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) names.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warned names: " + e.getMessage());
        }
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
        // Kicks and warns are instant actions — mark them inactive immediately
        boolean instant = type.equals("KICK") || type.equals("WARN");
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO cases (type, player_name, player_uuid, reason, issued_by, expires_at, chat_log, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, type);
            stmt.setString(2, playerName);
            stmt.setString(3, playerUuid);
            stmt.setString(4, reason);
            stmt.setString(5, issuedBy);
            stmt.setTimestamp(6, expiresAt);
            stmt.setString(7, chatLog);
            stmt.setInt(8, instant ? 0 : 1);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create case: " + e.getMessage());
        }
        return -1;
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
                            rs.getString("chat_log")
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
                            rs.getString("chat_log")
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
                            rs.getString("chat_log")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get cases by UUID: " + e.getMessage());
        }
        return cases;
    }

    public List<String> getKnownPlayerNames() {
        List<String> names = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT player_name FROM cases")) {
            while (rs.next()) {
                names.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get known player names: " + e.getMessage());
        }
        return names;
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
        }
    }

    public void deleteCase(int caseId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM cases WHERE id = ?")) {
            stmt.setInt(1, caseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete case: " + e.getMessage());
        }
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
        }
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
        }
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
                plugin.getLogger().info("Migrated " + updated + " KICK/WARN cases to resolved.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to migrate instant cases: " + e.getMessage());
        }
    }

    public long getPlaytimeMinutes(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT SUM(COALESCE(quit_time, ?) - join_time) as total FROM player_sessions WHERE player_uuid = ?")) {
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
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO frozen_players (player_uuid, frozen_by, frozen_at) VALUES (?, ?, ?)")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, frozenBy);
            stmt.setLong(3, System.currentTimeMillis());
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
                "SELECT 1 FROM frozen_players WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
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

    public Connection getConnection() {
        return connection;
    }
}
