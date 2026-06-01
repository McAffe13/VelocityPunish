package de.punishsystem.webhook;

import de.punishsystem.config.ConfigManager;
import de.punishsystem.config.WebhookConfig;
import de.punishsystem.punishment.IpBan;
import de.punishsystem.punishment.Punishment;
import de.punishsystem.utils.TimeParser;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class WebhookManager {

    // Discord-Farben
    private static final int COLOR_RED    = 0xFF0000;
    private static final int COLOR_ORANGE = 0xFF6B00;
    private static final int COLOR_GREEN  = 0x00CC44;
    private static final int COLOR_YELLOW = 0xFFCC00;
    private static final int COLOR_BLUE   = 0x0099FF;
    private static final int COLOR_PURPLE = 0x9B59B6;

    private final ConfigManager config;
    private final Logger logger;
    private final HttpClient httpClient;

    public WebhookManager(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void sendBanWebhook(Punishment p) {
        if (!isEnabled()) return;
        String url = config.getWebhooks().getBan();
        if (url.isBlank()) return;

        DiscordEmbed embed = new DiscordEmbed()
                .title("🔨 Player banned")
                .color(COLOR_RED)
                .addField("Player", p.getPlayerName(), true)
                .addField("Moderator", p.getModeratorName(), true)
                .addField("Ban-ID", "`" + p.getId() + "`", true)
                .addField("UUID", "`" + p.getPlayerUuid() + "`", false)
                .addField("Reason", p.getReason(), false)
                .addField("Duration", "Permanent", true)
                .addField("Date", formatDate(p.getStartTime()), false)
                .footer("PunishSystem")
                .timestampNow();

        sendAsync(url, embed);
    }

    public void sendTempBanWebhook(Punishment p) {
        if (!isEnabled()) return;
        String url = config.getWebhooks().getTempban();
        if (url.isBlank()) return;

        DiscordEmbed embed = new DiscordEmbed()
                .title("⏱️ Player temporarily banned")
                .color(COLOR_ORANGE)
                .addField("Player", p.getPlayerName(), true)
                .addField("Moderator", p.getModeratorName(), true)
                .addField("Ban-ID", "`" + p.getId() + "`", true)
                .addField("UUID", "`" + p.getPlayerUuid() + "`", false)
                .addField("Reason", p.getReason(), false)
                .addField("Duration", TimeParser.format(p.getDuration()), true)
                .addField("Expires", formatDate(p.getEndTime()), true)
                .footer("PunishSystem")
                .timestampNow();

        sendAsync(url, embed);
    }

    public void sendUnbanWebhook(Punishment p, String moderatorName) {
        if (!isEnabled()) return;
        String url = config.getWebhooks().getUnban();
        if (url.isBlank()) return;

        DiscordEmbed embed = new DiscordEmbed()
                .title("✅ Player unbanned")
                .color(COLOR_GREEN)
                .addField("Player", p.getPlayerName(), true)
                .addField("Unbanned by", moderatorName, true)
                .addField("Ban-ID", "`" + p.getId() + "`", true)
                .addField("Original reason", p.getReason(), false)
                .footer("PunishSystem")
                .timestampNow();

        sendAsync(url, embed);
    }

    public void sendKickWebhook(String playerName, String playerUuid, String moderatorName, String reason) {
        if (!isEnabled()) return;
        String url = config.getWebhooks().getKick();
        if (url.isBlank()) return;

        DiscordEmbed embed = new DiscordEmbed()
                .title("👢 Player kicked")
                .color(COLOR_BLUE)
                .addField("Player", playerName, true)
                .addField("Moderator", moderatorName, true)
                .addField("UUID", "`" + playerUuid + "`", false)
                .addField("Reason", reason, false)
                .footer("PunishSystem")
                .timestampNow();

        sendAsync(url, embed);
    }

    public void sendIpBanWebhook(IpBan ban) {
        if (!isEnabled()) return;
        String url = config.getWebhooks().getBan();
        if (url.isBlank()) return;

        DiscordEmbed embed = new DiscordEmbed()
                .title("🌐 IP banned")
                .color(COLOR_RED)
                .addField("IP address", "`" + ban.getIpAddress() + "`", true)
                .addField("Player", ban.getPlayerName() != null ? ban.getPlayerName() : "Unknown", true)
                .addField("Moderator", ban.getModeratorName(), true)
                .addField("Ban-ID", "`" + ban.getId() + "`", true)
                .addField("Reason", ban.getReason(), false)
                .footer("PunishSystem")
                .timestampNow();

        sendAsync(url, embed);
    }

    public void sendWarnWebhook(de.punishsystem.punishment.Punishment p) {
        if (!isEnabled()) return;
        String url = config.getWebhooks().getWarn();
        if (url.isBlank()) return;

        DiscordEmbed embed = new DiscordEmbed()
                .title("⚠️ Player warned")
                .color(COLOR_YELLOW)
                .addField("Player", p.getPlayerName(), true)
                .addField("Moderator", p.getModeratorName(), true)
                .addField("Warn-ID", "`" + p.getId() + "`", true)
                .addField("UUID", "`" + p.getPlayerUuid() + "`", false)
                .addField("Reason", p.getReason(), false)
                .addField("Date", formatDate(p.getStartTime()), false)
                .footer("PunishSystem")
                .timestampNow();

        sendAsync(url, embed);
    }

    public void sendVpnWebhook(String playerName, String playerUuid, String ip, String isp, String country) {
        if (!isEnabled()) return;
        String url = config.getWebhooks().getVpn();
        if (url.isBlank()) return;

        DiscordEmbed embed = new DiscordEmbed()
                .title("🛡️ VPN / Proxy detected")
                .color(COLOR_PURPLE)
                .addField("Player", playerName, true)
                .addField("UUID", "`" + playerUuid + "`", false)
                .addField("IP address", "`" + ip + "`", true)
                .addField("ISP", isp.isBlank() ? "Unknown" : isp, true)
                .addField("Country", country.isBlank() ? "Unknown" : country, true)
                .addField("Action", "KICK", true)
                .footer("PunishSystem")
                .timestampNow();

        sendAsync(url, embed);
    }

    private void sendAsync(String webhookUrl, DiscordEmbed embed) {
        String payload = embed.toWebhookPayload();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((resp, ex) -> {
                    if (ex != null) {
                        logger.warn("Webhook could not be sent: {}", ex.getMessage());
                    } else if (resp.statusCode() >= 400) {
                        logger.warn("Webhook gab HTTP {} zurück (URL: {})", resp.statusCode(), webhookUrl);
                    }
                });
    }

    private boolean isEnabled() {
        WebhookConfig cfg = config.getWebhooks();
        return cfg.isEnabled();
    }

    private String formatDate(long epochMs) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(epochMs));
    }
}
