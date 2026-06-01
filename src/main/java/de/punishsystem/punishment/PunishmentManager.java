package de.punishsystem.punishment;

import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.config.ConfigManager;
import de.punishsystem.config.MessagesConfig;
import de.punishsystem.storage.StorageManager;
import de.punishsystem.utils.BanIdGenerator;
import de.punishsystem.utils.MessageUtils;
import de.punishsystem.utils.TimeParser;
import de.punishsystem.webhook.WebhookManager;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentManager {

    private final PunishSystem plugin;
    private final StorageManager db;
    private final WebhookManager webhooks;
    private final ConfigManager config;
    private final Logger logger;

    public PunishmentManager(PunishSystem plugin, StorageManager db,
                              WebhookManager webhooks, ConfigManager config, Logger logger) {
        this.plugin = plugin;
        this.db = db;
        this.webhooks = webhooks;
        this.config = config;
        this.logger = logger;
    }

    // ── Ban ───────────────────────────────────────────────────────────────────

    public CompletableFuture<Punishment> ban(String playerName, UUID playerUuid,
                                              String moderatorName, UUID moderatorUuid,
                                              String reason) {
        long now = System.currentTimeMillis();
        String id = BanIdGenerator.generateBanId();

        Punishment p = new Punishment(id, playerUuid, playerName,
                moderatorUuid, moderatorName,
                PunishmentType.BAN, reason,
                -1L, now, -1L, true);

        return db.savePunishment(p).thenApply(v -> {
            kickIfOnline(playerUuid, buildBanScreen(p));
            webhooks.sendBanWebhook(p);
            notifyStaff(playerName, moderatorName, "BAN", reason);
            logger.info("[BAN] {} was banned by {}. Reason: {} | ID: {}",
                    playerName, moderatorName, reason, id);
            banLinkedAccounts(playerUuid, playerName, moderatorName, moderatorUuid, reason, PunishmentType.BAN, -1L);
            return p;
        });
    }

    public CompletableFuture<Punishment> tempBan(String playerName, UUID playerUuid,
                                                   String moderatorName, UUID moderatorUuid,
                                                   String reason, long durationMs) {
        long now = System.currentTimeMillis();
        long endTime = now + durationMs;
        String id = BanIdGenerator.generateBanId();

        Punishment p = new Punishment(id, playerUuid, playerName,
                moderatorUuid, moderatorName,
                PunishmentType.TEMPBAN, reason,
                durationMs, now, endTime, true);

        return db.savePunishment(p).thenApply(v -> {
            kickIfOnline(playerUuid, buildTempBanScreen(p));
            webhooks.sendTempBanWebhook(p);
            notifyStaff(playerName, moderatorName, "TEMPBAN", reason);
            logger.info("[TEMPBAN] {} was banned by {} for {}. Reason: {} | ID: {}",
                    playerName, moderatorName, TimeParser.format(durationMs), reason, id);
            banLinkedAccounts(playerUuid, playerName, moderatorName, moderatorUuid, reason, PunishmentType.TEMPBAN, durationMs);
            return p;
        });
    }

    // ── Network Ban ───────────────────────────────────────────────────────────

    public CompletableFuture<Punishment> networkBan(String playerName, UUID playerUuid,
                                                     String moderatorName, UUID moderatorUuid,
                                                     String reason) {
        long now = System.currentTimeMillis();
        String id = BanIdGenerator.generateBanId();

        Punishment p = new Punishment(id, playerUuid, playerName,
                moderatorUuid, moderatorName,
                PunishmentType.NETWORK_BAN, reason,
                -1L, now, -1L, true);

        return db.savePunishment(p).thenApply(v -> {
            kickIfOnline(playerUuid, buildNetworkBanScreen(p));
            notifyStaff(playerName, moderatorName, "NETWORK_BAN", reason);
            logger.info("[NETWORK_BAN] {} received a network ban from {}. Reason: {} | ID: {}",
                    playerName, moderatorName, reason, id);
            banLinkedAccounts(playerUuid, playerName, moderatorName, moderatorUuid, reason, PunishmentType.NETWORK_BAN, -1L);
            return p;
        });
    }

    // ── IP-Ban ────────────────────────────────────────────────────────────────

    public CompletableFuture<IpBan> ipBan(String playerName, UUID playerUuid,
                                           String ipAddress, String moderatorName,
                                           String reason) {
        long now = System.currentTimeMillis();
        String id = BanIdGenerator.generateBanId();

        IpBan ban = new IpBan(id, ipAddress, playerUuid, playerName,
                moderatorName, reason, now, -1L, true);

        return db.saveIpBan(ban).thenApply(v -> {
            kickIfOnline(playerUuid, buildIpBanScreen(ban));
            webhooks.sendIpBanWebhook(ban);
            logger.info("[IPBAN] IP {} ({}) was banned by {}. Reason: {} | ID: {}",
                    ipAddress, playerName, moderatorName, reason, id);
            return ban;
        });
    }

    // ── Unban ─────────────────────────────────────────────────────────────────

    public CompletableFuture<Boolean> unban(UUID playerUuid, String moderatorName) {
        return db.getActiveBan(playerUuid).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            Punishment p = opt.get();
            return db.deactivateBansForPlayer(playerUuid).thenApply(rows -> {
                if (rows > 0) {
                    webhooks.sendUnbanWebhook(p, moderatorName);
                    logger.info("[UNBAN] {} was unbanned by {}.", p.getPlayerName(), moderatorName);
                }
                return rows > 0;
            });
        });
    }

    // ── Warn ──────────────────────────────────────────────────────────────────

    public CompletableFuture<Punishment> warn(String playerName, UUID playerUuid,
                                              String moderatorName, UUID moderatorUuid,
                                              String reason) {
        long now = System.currentTimeMillis();
        String id = BanIdGenerator.generateBanId();

        Punishment p = new Punishment(id, playerUuid, playerName,
                moderatorUuid, moderatorName,
                PunishmentType.WARN, reason,
                -1L, now, -1L, true);

        return db.savePunishment(p).thenApply(v -> {
            messageIfOnline(playerUuid, buildWarnMessage(p));
            webhooks.sendWarnWebhook(p);
            notifyStaff(playerName, moderatorName, "WARN", reason);
            logger.info("[WARN] {} was warned by {}. Reason: {} | ID: {}",
                    playerName, moderatorName, reason, id);
            return p;
        });
    }

    public CompletableFuture<List<Punishment>> getActiveWarnings(UUID playerUuid) {
        return db.getActiveWarnings(playerUuid);
    }

    public CompletableFuture<Integer> clearWarnings(UUID playerUuid) {
        return db.clearWarnings(playerUuid);
    }

    // ── Kick ──────────────────────────────────────────────────────────────────

    public CompletableFuture<Boolean> kick(UUID playerUuid, String playerName,
                                            String moderatorName, String reason) {
        Optional<Player> opt = plugin.getServer().getPlayer(playerUuid);
        if (opt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        Player player = opt.get();
        player.disconnect(buildKickScreen(reason));
        webhooks.sendKickWebhook(playerName, playerUuid.toString(), moderatorName, reason);
        notifyStaff(playerName, moderatorName, "KICK", reason);
        logger.info("[KICK] {} was kicked by {}. Reason: {}", playerName, moderatorName, reason);
        return CompletableFuture.completedFuture(true);
    }

    // ── Abfragen ──────────────────────────────────────────────────────────────

    public CompletableFuture<Optional<Punishment>> getActiveBan(UUID playerUuid) {
        return db.getActiveBan(playerUuid);
    }

    public CompletableFuture<Optional<IpBan>> getActiveIpBan(String ip) {
        return db.getActiveIpBan(ip);
    }

    // ── Ban-Screen-Builder ────────────────────────────────────────────────────

    public Component buildBanScreen(Punishment p) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(p.getStartTime()));
        return MessageUtils.parse(
                config.getMessages().getBanScreen(),
                Map.of(
                        "reason", p.getReason(),
                        "id", p.getId(),
                        "discord_url", config.getNetwork().getDiscordUrl(),
                        "network", config.getNetwork().getName(),
                        "date", date
                )
        );
    }

    public Component buildTempBanScreen(Punishment p) {
        String expire = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(p.getEndTime()));
        return MessageUtils.parse(
                config.getMessages().getTempBanScreen(),
                Map.of(
                        "reason", p.getReason(),
                        "id", p.getId(),
                        "duration", TimeParser.format(p.getDuration()),
                        "expire", expire,
                        "network", config.getNetwork().getName()
                )
        );
    }

    public Component buildIpBanScreen(IpBan ban) {
        return MessageUtils.parse(
                config.getMessages().getIpBanScreen(),
                Map.of(
                        "reason", ban.getReason(),
                        "id", ban.getId(),
                        "discord_url", config.getNetwork().getDiscordUrl(),
                        "network", config.getNetwork().getName()
                )
        );
    }

    public Component buildVpnKickScreen() {
        return MessageUtils.parse(
                config.getMessages().getVpnKickScreen(),
                Map.of(
                        "network", config.getNetwork().getName(),
                        "discord_url", config.getNetwork().getDiscordUrl()
                )
        );
    }

    public Component buildKickScreen(String reason) {
        return MessageUtils.parse(
                config.getMessages().getKickScreen(),
                Map.of(
                        "reason", reason,
                        "network", config.getNetwork().getName()
                )
        );
    }

    public Component buildWarnMessage(Punishment p) {
        return MessageUtils.parse(
                config.getMessages().getWarnMessage(),
                Map.of(
                        "reason", p.getReason(),
                        "id", p.getId(),
                        "network", config.getNetwork().getName()
                )
        );
    }

    public Component buildNetworkBanScreen(Punishment p) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(p.getStartTime()));
        return MessageUtils.parse(
                config.getMessages().getNetworkBanScreen(),
                Map.of(
                        "reason", p.getReason(),
                        "id", p.getId(),
                        "discord_url", config.getNetwork().getDiscordUrl(),
                        "network", config.getNetwork().getName(),
                        "date", date
                )
        );
    }

    public Component buildMultiAccountScreen(Punishment bannedAccount) {
        return MessageUtils.parse(
                config.getMessages().getMultiAccountScreen(),
                Map.of(
                        "banned_player", bannedAccount.getPlayerName(),
                        "reason", bannedAccount.getReason(),
                        "discord_url", config.getNetwork().getDiscordUrl(),
                        "network", config.getNetwork().getName()
                )
        );
    }

    // ── Multi-Account Auto-Ban ────────────────────────────────────────────────

    private void banLinkedAccounts(UUID bannedUuid, String bannedName,
                                    String moderatorName, UUID moderatorUuid,
                                    String reason, PunishmentType type, long durationMs) {
        db.getLinkedAccounts(bannedUuid).thenAccept(linked -> {
            for (String[] account : linked) {
                UUID linkedUuid = UUID.fromString(account[0]);
                String linkedName = account[1];
                db.getActiveBan(linkedUuid).thenAccept(existingBan -> {
                    if (existingBan.isPresent() && !existingBan.get().isExpired()) return;

                    long now = System.currentTimeMillis();
                    String id = BanIdGenerator.generateBanId();
                    String altReason = "Alt account of " + bannedName + " | " + reason;
                    long end = durationMs > 0 ? now + durationMs : -1L;

                    Punishment altBan = new Punishment(id, linkedUuid, linkedName,
                            moderatorUuid, moderatorName,
                            type, altReason, durationMs, now, end, true);

                    db.savePunishment(altBan).thenRun(() -> {
                        Component screen = switch (type) {
                            case TEMPBAN -> buildTempBanScreen(altBan);
                            case NETWORK_BAN -> buildNetworkBanScreen(altBan);
                            default -> buildBanScreen(altBan);
                        };
                        kickIfOnline(linkedUuid, screen);
                        notifyStaff(linkedName, moderatorName, type.name(), altReason);
                        logger.info("[MULTI-ACCOUNT-BAN] {} was automatically banned (linked account of {}). ID: {}",
                                linkedName, bannedName, id);
                    }).exceptionally(ex -> {
                        logger.error("Error auto-banning linked account {}", linkedName, ex);
                        return null;
                    });
                }).exceptionally(ex -> {
                    logger.error("Error checking ban for linked account {}", linkedName, ex);
                    return null;
                });
            }
        }).exceptionally(ex -> {
            logger.error("Error fetching linked accounts for {}", bannedName, ex);
            return null;
        });
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private void kickIfOnline(UUID uuid, Component message) {
        plugin.getServer().getPlayer(uuid).ifPresent(p -> p.disconnect(message));
    }

    private void messageIfOnline(UUID uuid, Component message) {
        plugin.getServer().getPlayer(uuid).ifPresent(p -> p.sendMessage(message));
    }

    private void notifyStaff(String player, String mod, String type, String reason) {
        MessagesConfig msg = config.getMessages();
        String notify = MessageUtils.replace(
                msg.getStaffNotify(),
                Map.of("player", player, "mod", mod, "type", type, "reason", reason)
        );
        Component notifyComponent = MessageUtils.parse(notify);
        plugin.getServer().getAllPlayers().stream()
                .filter(p -> p.hasPermission(config.getPermissions().getNotify()))
                .forEach(p -> p.sendMessage(notifyComponent));
    }
}
