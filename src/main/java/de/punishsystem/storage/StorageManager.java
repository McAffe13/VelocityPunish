package de.punishsystem.storage;

import de.punishsystem.punishment.IpBan;
import de.punishsystem.punishment.Punishment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageManager {

    void initialize();
    void shutdown();

    CompletableFuture<Void> savePunishment(Punishment p);
    CompletableFuture<Optional<Punishment>> getActiveBan(UUID playerUuid);
    CompletableFuture<Optional<Punishment>> getPunishmentById(String id);
    CompletableFuture<Void> deactivatePunishment(String id);
    CompletableFuture<Integer> deactivateBansForPlayer(UUID playerUuid);
    CompletableFuture<List<Punishment>> getPlayerHistory(UUID playerUuid);

    CompletableFuture<Void> saveIpBan(IpBan ban);
    CompletableFuture<Optional<IpBan>> getActiveIpBan(String ipAddress);
    CompletableFuture<Void> deactivateIpBan(String id);

    CompletableFuture<Void> saveVpnLog(String playerUuid, String playerName,
                                       String ip, String isp, String countryCode);

    CompletableFuture<Void> savePlayerIp(UUID playerUuid, String playerName, String ip);
    CompletableFuture<List<String>> getPlayerIps(UUID playerUuid);
    CompletableFuture<List<String[]>> getLinkedAccounts(UUID playerUuid);
    CompletableFuture<Optional<Punishment>> getBannedAccountForPlayer(UUID playerUuid);
    CompletableFuture<Optional<Long>> getPlayerLastSeen(UUID playerUuid);
    CompletableFuture<Integer> getPunishmentCount(UUID playerUuid);
    CompletableFuture<Optional<UUID>> getPlayerUuidByName(String playerName);

    CompletableFuture<List<Punishment>> getActiveWarnings(UUID playerUuid);
    CompletableFuture<Integer> clearWarnings(UUID playerUuid);
}
