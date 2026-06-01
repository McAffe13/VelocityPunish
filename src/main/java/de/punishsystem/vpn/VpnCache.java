package de.punishsystem.vpn;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class VpnCache {

    private final Map<String, VpnResult> cache = new ConcurrentHashMap<>();
    private final long ttlMs;

    public VpnCache(int expireMinutes) {
        this.ttlMs = (long) expireMinutes * 60_000L;
    }

    public void put(String ip, VpnResult result) {
        cache.put(ip, result);
    }

    public Optional<VpnResult> get(String ip) {
        VpnResult result = cache.get(ip);
        if (result == null) return Optional.empty();

        if (System.currentTimeMillis() - result.getCheckedAt() > ttlMs) {
            cache.remove(ip);
            return Optional.empty();
        }

        return Optional.of(result);
    }

    public void invalidate(String ip) {
        cache.remove(ip);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
