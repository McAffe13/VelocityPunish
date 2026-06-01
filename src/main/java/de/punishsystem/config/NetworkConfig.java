package de.punishsystem.config;

public class NetworkConfig {

    private String name = "YourNetwork";
    private String nameFormatted = "§6§lYourNetwork";
    private String prefix = "§8[§6PunishSystem§8] §7";
    private String websiteUrl = "your-server.net";
    private String unbanUrl = "your-server.net/appeal";
    private String discordUrl = "discord.your-server.net";
    private String unbanMethod = "discord";

    public String getName() { return name; }
    public String getNameFormatted() { return nameFormatted; }
    public String getPrefix() { return prefix; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getUnbanUrl() { return unbanUrl; }
    public String getDiscordUrl() { return discordUrl; }
    public String getUnbanMethod() { return unbanMethod; }
}
