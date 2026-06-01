package de.punishsystem.vpn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.punishsystem.config.ConfigManager;
import de.punishsystem.config.VpnConfig;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VpnChecker {

    private final ConfigManager config;
    private final Logger logger;
    private final HttpClient httpClient;
    private VpnCache cache;

    // Rate-Limiter: maximal 40 Anfragen pro Minute (ip-api.com Free: 45/min)
    private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
    private final ScheduledExecutorService rateLimitReset = Executors.newSingleThreadScheduledExecutor();

    public VpnChecker(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.cache = new VpnCache(config.getVpn().getCacheExpireMinutes());

        rateLimitReset.scheduleAtFixedRate(
                () -> requestsThisMinute.set(0),
                1, 1, TimeUnit.MINUTES
        );
    }

    /**
     * Checks an IP address for VPN/proxy usage.
     * Returns a cached result if available.
     */
    public CompletableFuture<VpnResult> check(String ip) {
        VpnConfig vpnConfig = config.getVpn();

        // Skip localhost / private IPs
        if (isPrivateAddress(ip)) {
            return CompletableFuture.completedFuture(new VpnResult(false, "Local", ""));
        }

        // Check cache
        var cached = cache.get(ip);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        // Check rate limit
        if (requestsThisMinute.get() >= 40) {
            logger.warn("VPN rate limit reached for IP: {}", ip);
            return CompletableFuture.completedFuture(new VpnResult(false, "", ""));
        }

        String url = vpnConfig.getApiUrl().replace("{ip}", ip);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        requestsThisMinute.incrementAndGet();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warn("VPN API returned HTTP {} for {}", response.statusCode(), ip);
                        return new VpnResult(false, "", "");
                    }
                    return parseResponse(response.body(), ip, vpnConfig.getWhitelistedISPs());
                })
                .thenApply(result -> {
                    cache.put(ip, result);
                    return result;
                })
                .exceptionally(ex -> {
                    logger.warn("VPN check for {} failed: {}", ip, ex.getMessage());
                    return new VpnResult(false, "", "");
                });
    }

    private VpnResult parseResponse(String body, String ip, List<String> whitelistedISPs) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String status = json.has("status") ? json.get("status").getAsString() : "fail";
            if (!"success".equals(status)) {
                return new VpnResult(false, "", "");
            }

            boolean isProxy = json.has("proxy") && json.get("proxy").getAsBoolean();
            boolean isHosting = json.has("hosting") && json.get("hosting").getAsBoolean();
            String isp = json.has("isp") ? json.get("isp").getAsString() : "";
            String countryCode = json.has("countryCode") ? json.get("countryCode").getAsString() : "";

            // Check whitelist
            if (!whitelistedISPs.isEmpty()) {
                String ispLower = isp.toLowerCase();
                for (String whitelisted : whitelistedISPs) {
                    if (ispLower.contains(whitelisted.toLowerCase())) {
                        return new VpnResult(false, isp, countryCode);
                    }
                }
            }

            return new VpnResult(isProxy || isHosting, isp, countryCode);

        } catch (Exception e) {
            logger.warn("Error parsing VPN API response for {}: {}", ip, e.getMessage());
            return new VpnResult(false, "", "");
        }
    }

    private boolean isPrivateAddress(String ip) {
        return ip.equals("127.0.0.1")
                || ip.equals("::1")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.");
    }

    public void shutdown() {
        rateLimitReset.shutdown();
        cache.clear();
    }
}
