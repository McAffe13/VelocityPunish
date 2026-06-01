package de.punishsystem.config;

public class WebhookConfig {

    private boolean enabled = false;
    private String ban = "";
    private String tempban = "";
    private String unban = "";
    private String kick = "";
    private String vpn = "";
    private String warn = "";

    public boolean isEnabled() { return enabled; }
    public String getBan() { return ban; }
    public String getTempban() { return tempban; }
    public String getUnban() { return unban; }
    public String getKick() { return kick; }
    public String getVpn() { return vpn; }
    public String getWarn() { return warn; }
}
