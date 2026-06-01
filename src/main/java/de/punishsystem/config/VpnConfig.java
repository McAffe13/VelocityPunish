package de.punishsystem.config;

import java.util.ArrayList;
import java.util.List;

public class VpnConfig {

    private boolean enabled = true;
    private boolean kickOnDetect = true;
    private String apiUrl = "http://ip-api.com/json/{ip}?fields=status,message,proxy,hosting,isp,countryCode";
    private int cacheExpireMinutes = 60;
    private List<String> whitelistedISPs = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public boolean isKickOnDetect() { return kickOnDetect; }
    public String getApiUrl() { return apiUrl; }
    public int getCacheExpireMinutes() { return cacheExpireMinutes; }
    public List<String> getWhitelistedISPs() { return whitelistedISPs; }
}
