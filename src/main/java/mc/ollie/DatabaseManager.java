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
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO cases (type, player_name, player_uuid, reason, issued_by, expires_at, chat_log) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, type);
            stmt.setString(2, playerName);
            stmt.setString(3, playerUuid);
            stmt.setString(4, reason);
            stmt.setString(5, issuedBy);
            stmt.setTimestamp(6, expiresAt);
            stmt.setString(7, chatLog);
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
                "SELECT COUNT(*) FROM cases WHERE LOWER(player_name) = LOWER(?)")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get case count: " + e.getMessage());
        }
        return 0;
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
}
