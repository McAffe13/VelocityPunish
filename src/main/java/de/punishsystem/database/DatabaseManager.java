package de.punishsystem.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.punishsystem.config.ConfigManager;
import de.punishsystem.config.DatabaseConfig;
import de.punishsystem.punishment.IpBan;
import de.punishsystem.punishment.Punishment;
import de.punishsystem.punishment.PunishmentType;
import de.punishsystem.storage.StorageManager;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager implements StorageManager {

    private final ConfigManager config;
    private final Logger logger;
    private HikariDataSource dataSource;
    private String prefix;

    public DatabaseManager(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void initialize() {
        DatabaseConfig db = config.getDatabase();
        this.prefix = db.getTablePrefix();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found!", e);
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setJdbcUrl(db.getJdbcUrl());
        hikari.setUsername(db.getUsername());
        hikari.setPassword(db.getPassword());
        hikari.setMaximumPoolSize(db.getMaxPoolSize());
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(30_000);
        hikari.setIdleTimeout(600_000);
        hikari.setMaxLifetime(1_800_000);
        hikari.setPoolName("PunishSystem-Pool");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(hikari);
        createTables();
        logger.info("Database connection established.");
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection closed.");
        }
    }

    // ── Create Tables ─────────────────────────────────────────────────────────

    private void createTables() {
        String punishments = """
                CREATE TABLE IF NOT EXISTS `%spunishments` (
                    `id` VARCHAR(8) NOT NULL,
                    `player_uuid` VARCHAR(36) NOT NULL,
                    `player_name` VARCHAR(16) NOT NULL,
                    `moderator_uuid` VARCHAR(36) DEFAULT NULL,
                    `moderator_name` VARCHAR(32) NOT NULL DEFAULT 'Console',
                    `type` VARCHAR(20) NOT NULL,
                    `reason` TEXT NOT NULL,
                    `duration` BIGINT NOT NULL DEFAULT -1,
                    `start_time` BIGINT NOT NULL,
                    `end_time` BIGINT NOT NULL DEFAULT -1,
                    `active` TINYINT(1) NOT NULL DEFAULT 1,
                    PRIMARY KEY (`id`),
                    INDEX `idx_player_uuid` (`player_uuid`),
                    INDEX `idx_active` (`active`),
                    INDEX `idx_type` (`type`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """.formatted(prefix);

        String ipBans = """
                CREATE TABLE IF NOT EXISTS `%sip_bans` (
                    `id` VARCHAR(8) NOT NULL,
                    `ip_address` VARCHAR(45) NOT NULL,
                    `player_uuid` VARCHAR(36) DEFAULT NULL,
                    `player_name` VARCHAR(16) DEFAULT NULL,
                    `moderator_name` VARCHAR(32) NOT NULL DEFAULT 'Console',
                    `reason` TEXT NOT NULL,
                    `start_time` BIGINT NOT NULL,
                    `end_time` BIGINT NOT NULL DEFAULT -1,
                    `active` TINYINT(1) NOT NULL DEFAULT 1,
                    PRIMARY KEY (`id`),
                    INDEX `idx_ip` (`ip_address`),
                    INDEX `idx_active` (`active`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """.formatted(prefix);

        String vpnLogs = """
                CREATE TABLE IF NOT EXISTS `%svpn_logs` (
                    `id` INT NOT NULL AUTO_INCREMENT,
                    `player_uuid` VARCHAR(36) NOT NULL,
                    `player_name` VARCHAR(16) NOT NULL,
                    `ip_address` VARCHAR(45) NOT NULL,
                    `isp` VARCHAR(255) DEFAULT NULL,
                    `country_code` VARCHAR(8) DEFAULT NULL,
                    `detected_at` BIGINT NOT NULL,
                    `action_taken` VARCHAR(20) NOT NULL DEFAULT 'KICK',
                    PRIMARY KEY (`id`),
                    INDEX `idx_player` (`player_uuid`),
                    INDEX `idx_ip` (`ip_address`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """.formatted(prefix);

        String playerIps = """
                CREATE TABLE IF NOT EXISTS `%splayer_ips` (
                    `player_uuid` VARCHAR(36) NOT NULL,
                    `player_name` VARCHAR(16) NOT NULL,
                    `ip_address` VARCHAR(45) NOT NULL,
                    `first_seen` BIGINT NOT NULL,
                    `last_seen` BIGINT NOT NULL,
                    PRIMARY KEY (`player_uuid`, `ip_address`),
                    INDEX `idx_ip` (`ip_address`),
                    INDEX `idx_uuid` (`player_uuid`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """.formatted(prefix);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(punishments);
            stmt.execute(ipBans);
            stmt.execute(vpnLogs);
            stmt.execute(playerIps);
        } catch (SQLException e) {
            logger.error("Error creating tables!", e);
            throw new RuntimeException(e);
        }
    }

    // ── Bans / Punishments ────────────────────────────────────────────────────

    public CompletableFuture<Void> savePunishment(Punishment p) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO `" + prefix + "punishments` " +
                    "(id, player_uuid, player_name, moderator_uuid, moderator_name, " +
                    "type, reason, duration, start_time, end_time, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getId());
                ps.setString(2, p.getPlayerUuid().toString());
                ps.setString(3, p.getPlayerName());
                ps.setString(4, p.getModeratorUuid() != null ? p.getModeratorUuid().toString() : null);
                ps.setString(5, p.getModeratorName());
                ps.setString(6, p.getType().name());
                ps.setString(7, p.getReason());
                ps.setLong(8, p.getDuration());
                ps.setLong(9, p.getStartTime());
                ps.setLong(10, p.getEndTime());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<Punishment>> getActiveBan(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + prefix + "punishments` " +
                    "WHERE player_uuid = ? AND active = 1 " +
                    "AND (type = 'BAN' OR type = 'TEMPBAN' OR type = 'NETWORK_BAN') " +
                    "ORDER BY start_time DESC LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapPunishment(rs));
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<Punishment>> getPunishmentById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + prefix + "punishments` WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapPunishment(rs));
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> deactivatePunishment(String id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE `" + prefix + "punishments` SET active = 0 WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Integer> deactivateBansForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + prefix + "punishments` SET active = 0 " +
                    "WHERE player_uuid = ? AND active = 1 " +
                    "AND (type = 'BAN' OR type = 'TEMPBAN' OR type = 'NETWORK_BAN')";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<Punishment>> getPlayerHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + prefix + "punishments` " +
                    "WHERE player_uuid = ? ORDER BY start_time DESC LIMIT 50";
            List<Punishment> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(mapPunishment(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return list;
        });
    }

    // ── IP-Bans ──────────────────────────────────────────────────────────────

    public CompletableFuture<Void> saveIpBan(IpBan ban) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO `" + prefix + "ip_bans` " +
                    "(id, ip_address, player_uuid, player_name, moderator_name, " +
                    "reason, start_time, end_time, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ban.getId());
                ps.setString(2, ban.getIpAddress());
                ps.setString(3, ban.getPlayerUuid() != null ? ban.getPlayerUuid().toString() : null);
                ps.setString(4, ban.getPlayerName());
                ps.setString(5, ban.getModeratorName());
                ps.setString(6, ban.getReason());
                ps.setLong(7, ban.getStartTime());
                ps.setLong(8, ban.getEndTime());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<IpBan>> getActiveIpBan(String ipAddress) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + prefix + "ip_bans` " +
                    "WHERE ip_address = ? AND active = 1 " +
                    "ORDER BY start_time DESC LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ipAddress);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapIpBan(rs));
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> deactivateIpBan(String id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE `" + prefix + "ip_bans` SET active = 0 WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ── VPN Logs ──────────────────────────────────────────────────────────────

    public CompletableFuture<Void> saveVpnLog(String playerUuid, String playerName,
                                               String ip, String isp, String countryCode) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO `" + prefix + "vpn_logs` " +
                    "(player_uuid, player_name, ip_address, isp, country_code, detected_at, action_taken) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'KICK')";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid);
                ps.setString(2, playerName);
                ps.setString(3, ip);
                ps.setString(4, isp);
                ps.setString(5, countryCode);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ── Player-IP Tracking ────────────────────────────────────────────────────

    public CompletableFuture<Void> savePlayerIp(UUID playerUuid, String playerName, String ip) {
        return CompletableFuture.runAsync(() -> {
            long now = System.currentTimeMillis();
            String sql = "INSERT INTO `" + prefix + "player_ips` " +
                    "(player_uuid, player_name, ip_address, first_seen, last_seen) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE player_name = ?, last_seen = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerName);
                ps.setString(3, ip);
                ps.setLong(4, now);
                ps.setLong(5, now);
                ps.setString(6, playerName);
                ps.setLong(7, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Returns all known IPs of a player. */
    public CompletableFuture<List<String>> getPlayerIps(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT ip_address FROM `" + prefix + "player_ips` " +
                    "WHERE player_uuid = ? ORDER BY last_seen DESC";
            List<String> ips = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) ips.add(rs.getString("ip_address"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return ips;
        });
    }

    /** Returns all players who have shared an IP with the given player (excluding themselves). */
    public CompletableFuture<List<String[]>> getLinkedAccounts(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT DISTINCT pi2.player_uuid, pi2.player_name " +
                    "FROM `" + prefix + "player_ips` pi1 " +
                    "JOIN `" + prefix + "player_ips` pi2 ON pi1.ip_address = pi2.ip_address " +
                    "WHERE pi1.player_uuid = ? AND pi2.player_uuid != ?";
            List<String[]> accounts = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    accounts.add(new String[]{rs.getString("player_uuid"), rs.getString("player_name")});
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return accounts;
        });
    }

    /**
     * Checks if any account that has ever shared an IP with this player
     * has an active ban (BAN, TEMPBAN or NETWORK_BAN).
     */
    public CompletableFuture<Optional<Punishment>> getBannedAccountForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT p.* FROM `" + prefix + "punishments` p " +
                    "JOIN `" + prefix + "player_ips` pi ON p.player_uuid = pi.player_uuid " +
                    "WHERE p.active = 1 " +
                    "AND (p.type = 'BAN' OR p.type = 'TEMPBAN' OR p.type = 'NETWORK_BAN') " +
                    "AND p.player_uuid != ? " +
                    "AND (p.end_time = -1 OR p.end_time > ?) " +
                    "AND pi.ip_address IN (" +
                    "  SELECT ip_address FROM `" + prefix + "player_ips` WHERE player_uuid = ?" +
                    ") " +
                    "ORDER BY p.start_time DESC LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapPunishment(rs));
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Gibt den Zeitstempel des letzten Logins zurück (aus player_ips). */
    public CompletableFuture<Optional<Long>> getPlayerLastSeen(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT MAX(last_seen) AS last_seen FROM `" + prefix + "player_ips` " +
                    "WHERE player_uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long val = rs.getLong("last_seen");
                    return val > 0 ? Optional.of(val) : Optional.empty();
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Returns the total number of punishments for a player. */
    public CompletableFuture<Integer> getPunishmentCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + prefix + "punishments` WHERE player_uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ── Warnings ──────────────────────────────────────────────────────────────

    public CompletableFuture<List<Punishment>> getActiveWarnings(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + prefix + "punishments` " +
                    "WHERE player_uuid = ? AND type = 'WARN' AND active = 1 " +
                    "ORDER BY start_time DESC";
            List<Punishment> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapPunishment(rs));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return list;
        });
    }

    public CompletableFuture<Integer> clearWarnings(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + prefix + "punishments` SET active = 0 " +
                    "WHERE player_uuid = ? AND type = 'WARN' AND active = 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ── Player Lookup ─────────────────────────────────────────────────────────

    public CompletableFuture<Optional<UUID>> getPlayerUuidByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player_uuid FROM `" + prefix + "punishments` " +
                    "WHERE player_name = ? ORDER BY start_time DESC LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(UUID.fromString(rs.getString("player_uuid")));
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.setId(rs.getString("id"));
        p.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        p.setPlayerName(rs.getString("player_name"));
        String modUuid = rs.getString("moderator_uuid");
        p.setModeratorUuid(modUuid != null ? UUID.fromString(modUuid) : null);
        p.setModeratorName(rs.getString("moderator_name"));
        p.setType(PunishmentType.valueOf(rs.getString("type")));
        p.setReason(rs.getString("reason"));
        p.setDuration(rs.getLong("duration"));
        p.setStartTime(rs.getLong("start_time"));
        p.setEndTime(rs.getLong("end_time"));
        p.setActive(rs.getBoolean("active"));
        return p;
    }

    private IpBan mapIpBan(ResultSet rs) throws SQLException {
        IpBan ban = new IpBan();
        ban.setId(rs.getString("id"));
        ban.setIpAddress(rs.getString("ip_address"));
        String playerUuid = rs.getString("player_uuid");
        ban.setPlayerUuid(playerUuid != null ? UUID.fromString(playerUuid) : null);
        ban.setPlayerName(rs.getString("player_name"));
        ban.setModeratorName(rs.getString("moderator_name"));
        ban.setReason(rs.getString("reason"));
        ban.setStartTime(rs.getLong("start_time"));
        ban.setEndTime(rs.getLong("end_time"));
        ban.setActive(rs.getBoolean("active"));
        return ban;
    }

}
