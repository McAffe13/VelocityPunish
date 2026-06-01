package de.punishsystem.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.punishsystem.punishment.IpBan;
import de.punishsystem.punishment.Punishment;
import de.punishsystem.punishment.PunishmentType;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FileStorageManager implements StorageManager {

    private final Path dataDirectory;
    private final Logger logger;
    private final Gson gson;

    private final List<Punishment> punishments = new ArrayList<>();
    private final List<IpBan> ipBans = new ArrayList<>();
    private final Map<String, PlayerData> playerData = new HashMap<>();

    public FileStorageManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(UUID.class,
                        (JsonSerializer<UUID>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(UUID.class,
                        (JsonDeserializer<UUID>) (json, t, ctx) -> UUID.fromString(json.getAsString()))
                .create();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize() {
        try {
            Files.createDirectories(dataDirectory);
            loadAll();
            logger.info("File storage initialized ({} punishments, {} IP-bans).",
                    punishments.size(), ipBans.size());
        } catch (IOException e) {
            throw new RuntimeException("Error initializing file storage!", e);
        }
    }

    @Override
    public void shutdown() {
        logger.info("File storage shut down.");
    }

    // ── Punishments ───────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<Void> savePunishment(Punishment p) {
        return CompletableFuture.runAsync(() -> {
            synchronized (punishments) {
                punishments.add(p);
                persistPunishments();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Punishment>> getActiveBan(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                return punishments.stream()
                        .filter(p -> p.getPlayerUuid().equals(playerUuid)
                                && p.isActive() && isBanType(p.getType()))
                        .max(Comparator.comparingLong(Punishment::getStartTime));
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Punishment>> getPunishmentById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                return punishments.stream().filter(p -> p.getId().equals(id)).findFirst();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deactivatePunishment(String id) {
        return CompletableFuture.runAsync(() -> {
            synchronized (punishments) {
                punishments.stream().filter(p -> p.getId().equals(id))
                        .forEach(p -> p.setActive(false));
                persistPunishments();
            }
        });
    }

    @Override
    public CompletableFuture<Integer> deactivateBansForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                int count = 0;
                for (Punishment p : punishments) {
                    if (p.getPlayerUuid().equals(playerUuid)
                            && p.isActive() && isBanType(p.getType())) {
                        p.setActive(false);
                        count++;
                    }
                }
                if (count > 0) persistPunishments();
                return count;
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getPlayerHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                return punishments.stream()
                        .filter(p -> p.getPlayerUuid().equals(playerUuid))
                        .sorted(Comparator.comparingLong(Punishment::getStartTime).reversed())
                        .limit(50)
                        .collect(Collectors.toList());
            }
        });
    }

    // ── IP-Bans ───────────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<Void> saveIpBan(IpBan ban) {
        return CompletableFuture.runAsync(() -> {
            synchronized (ipBans) {
                ipBans.add(ban);
                persistIpBans();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<IpBan>> getActiveIpBan(String ipAddress) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (ipBans) {
                return ipBans.stream()
                        .filter(b -> b.getIpAddress().equals(ipAddress) && b.isActive())
                        .max(Comparator.comparingLong(IpBan::getStartTime));
            }
        });
    }

    @Override
    public CompletableFuture<Void> deactivateIpBan(String id) {
        return CompletableFuture.runAsync(() -> {
            synchronized (ipBans) {
                ipBans.stream().filter(b -> b.getId().equals(id))
                        .forEach(b -> b.setActive(false));
                persistIpBans();
            }
        });
    }

    // ── VPN-Logs ──────────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<Void> saveVpnLog(String playerUuid, String playerName,
                                               String ip, String isp, String countryCode) {
        return CompletableFuture.completedFuture(null);
    }

    // ── Player IPs ────────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<Void> savePlayerIp(UUID playerUuid, String playerName, String ip) {
        return CompletableFuture.runAsync(() -> {
            synchronized (playerData) {
                PlayerData data = playerData.computeIfAbsent(
                        playerUuid.toString(), k -> new PlayerData());
                data.playerName = playerName;
                long now = System.currentTimeMillis();
                IpEntry existing = data.ips.get(ip);
                if (existing == null) {
                    data.ips.put(ip, new IpEntry(now));
                } else {
                    existing.lastSeen = now;
                }
                persistPlayerData();
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> getPlayerIps(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (playerData) {
                PlayerData data = playerData.get(playerUuid.toString());
                if (data == null) return Collections.emptyList();
                return data.ips.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue().lastSeen, a.getValue().lastSeen))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
            }
        });
    }

    @Override
    public CompletableFuture<List<String[]>> getLinkedAccounts(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (playerData) {
                String key = playerUuid.toString();
                PlayerData data = playerData.get(key);
                if (data == null) return Collections.emptyList();
                Set<String> knownIps = data.ips.keySet();
                List<String[]> result = new ArrayList<>();
                for (Map.Entry<String, PlayerData> entry : playerData.entrySet()) {
                    if (entry.getKey().equals(key)) continue;
                    for (String ip : entry.getValue().ips.keySet()) {
                        if (knownIps.contains(ip)) {
                            result.add(new String[]{entry.getKey(), entry.getValue().playerName});
                            break;
                        }
                    }
                }
                return result;
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Punishment>> getBannedAccountForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String[]> linked;
            synchronized (playerData) {
                String key = playerUuid.toString();
                PlayerData data = playerData.get(key);
                if (data == null) return Optional.empty();
                Set<String> knownIps = data.ips.keySet();
                linked = new ArrayList<>();
                for (Map.Entry<String, PlayerData> entry : playerData.entrySet()) {
                    if (entry.getKey().equals(key)) continue;
                    for (String ip : entry.getValue().ips.keySet()) {
                        if (knownIps.contains(ip)) {
                            linked.add(new String[]{entry.getKey(), entry.getValue().playerName});
                            break;
                        }
                    }
                }
            }

            if (linked.isEmpty()) return Optional.empty();

            long now = System.currentTimeMillis();
            synchronized (punishments) {
                for (String[] account : linked) {
                    UUID linkedUuid = UUID.fromString(account[0]);
                    Optional<Punishment> ban = punishments.stream()
                            .filter(p -> p.getPlayerUuid().equals(linkedUuid)
                                    && p.isActive()
                                    && isBanType(p.getType())
                                    && (p.getEndTime() == -1 || p.getEndTime() > now))
                            .max(Comparator.comparingLong(Punishment::getStartTime));
                    if (ban.isPresent()) return ban;
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<Long>> getPlayerLastSeen(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (playerData) {
                PlayerData data = playerData.get(playerUuid.toString());
                if (data == null) return Optional.empty();
                return data.ips.values().stream()
                        .mapToLong(e -> e.lastSeen)
                        .max()
                        .stream().boxed().findFirst();
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getPunishmentCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                return (int) punishments.stream()
                        .filter(p -> p.getPlayerUuid().equals(playerUuid))
                        .count();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<UUID>> getPlayerUuidByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                return punishments.stream()
                        .filter(p -> p.getPlayerName().equalsIgnoreCase(playerName))
                        .max(Comparator.comparingLong(Punishment::getStartTime))
                        .map(Punishment::getPlayerUuid);
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getActiveWarnings(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                return punishments.stream()
                        .filter(p -> p.getPlayerUuid().equals(playerUuid)
                                && p.getType() == PunishmentType.WARN
                                && p.isActive())
                        .sorted(Comparator.comparingLong(Punishment::getStartTime).reversed())
                        .collect(Collectors.toList());
            }
        });
    }

    @Override
    public CompletableFuture<Integer> clearWarnings(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (punishments) {
                int count = 0;
                for (Punishment p : punishments) {
                    if (p.getPlayerUuid().equals(playerUuid)
                            && p.getType() == PunishmentType.WARN
                            && p.isActive()) {
                        p.setActive(false);
                        count++;
                    }
                }
                if (count > 0) persistPunishments();
                return count;
            }
        });
    }

    // ── Persistenz ────────────────────────────────────────────────────────────

    private void loadAll() throws IOException {
        Path pFile = dataDirectory.resolve("punishments.json");
        if (Files.exists(pFile)) {
            Type type = new TypeToken<List<Punishment>>() {}.getType();
            List<Punishment> loaded = gson.fromJson(
                    Files.readString(pFile, StandardCharsets.UTF_8), type);
            if (loaded != null) punishments.addAll(loaded);
        }

        Path iFile = dataDirectory.resolve("ip_bans.json");
        if (Files.exists(iFile)) {
            Type type = new TypeToken<List<IpBan>>() {}.getType();
            List<IpBan> loaded = gson.fromJson(
                    Files.readString(iFile, StandardCharsets.UTF_8), type);
            if (loaded != null) ipBans.addAll(loaded);
        }

        Path pdFile = dataDirectory.resolve("player_ips.json");
        if (Files.exists(pdFile)) {
            Type type = new TypeToken<Map<String, PlayerData>>() {}.getType();
            Map<String, PlayerData> loaded = gson.fromJson(
                    Files.readString(pdFile, StandardCharsets.UTF_8), type);
            if (loaded != null) playerData.putAll(loaded);
        }
    }

    private void persistPunishments() {
        persist("punishments.json", punishments);
    }

    private void persistIpBans() {
        persist("ip_bans.json", ipBans);
    }

    private void persistPlayerData() {
        persist("player_ips.json", playerData);
    }

    private void persist(String filename, Object data) {
        try {
            Files.writeString(dataDirectory.resolve(filename),
                    gson.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error saving {}!", filename, e);
        }
    }

    private boolean isBanType(PunishmentType type) {
        return type == PunishmentType.BAN
                || type == PunishmentType.TEMPBAN
                || type == PunishmentType.NETWORK_BAN;
    }

    // ── Innere Klassen ────────────────────────────────────────────────────────

    private static class PlayerData {
        String playerName;
        Map<String, IpEntry> ips = new HashMap<>();
    }

    private static class IpEntry {
        long lastSeen;

        IpEntry(long lastSeen) {
            this.lastSeen = lastSeen;
        }
    }
}
