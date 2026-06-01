package de.punishsystem.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MojangApi {

    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Map<String, UUID> CACHE = new ConcurrentHashMap<>();

    private MojangApi() {}

    /**
     * Schlägt die UUID eines Spielers via Mojang-API nach.
     * Ergebnis wird gecacht.
     */
    public static CompletableFuture<Optional<UUID>> fetchUuid(String playerName) {
        String lower = playerName.toLowerCase();

        if (CACHE.containsKey(lower)) {
            return CompletableFuture.completedFuture(Optional.of(CACHE.get(lower)));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PROFILE_URL + playerName))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 204 || response.statusCode() == 404) {
                        return Optional.<UUID>empty();
                    }
                    if (response.statusCode() != 200) {
                        return Optional.<UUID>empty();
                    }
                    try {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        String rawId = json.get("id").getAsString();
                        UUID uuid = insertDashes(rawId);
                        CACHE.put(lower, uuid);
                        return Optional.of(uuid);
                    } catch (Exception e) {
                        return Optional.<UUID>empty();
                    }
                })
                .exceptionally(e -> Optional.empty());
    }

    private static UUID insertDashes(String raw) {
        String formatted = raw.substring(0, 8) + "-"
                + raw.substring(8, 12) + "-"
                + raw.substring(12, 16) + "-"
                + raw.substring(16, 20) + "-"
                + raw.substring(20);
        return UUID.fromString(formatted);
    }
}
