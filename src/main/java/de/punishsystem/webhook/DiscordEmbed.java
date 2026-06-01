package de.punishsystem.webhook;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiscordEmbed {

    private String title;
    private String description;
    private int color;
    private final List<Field> fields = new ArrayList<>();
    private String timestamp;
    private String footerText;
    private String footerIconUrl;

    public static class Field {
        final String name;
        final String value;
        final boolean inline;

        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value.isEmpty() ? "​" : value;
            this.inline = inline;
        }
    }

    public DiscordEmbed title(String title) {
        this.title = title;
        return this;
    }

    public DiscordEmbed description(String description) {
        this.description = description;
        return this;
    }

    public DiscordEmbed color(int color) {
        this.color = color;
        return this;
    }

    public DiscordEmbed addField(String name, String value, boolean inline) {
        fields.add(new Field(name, value, inline));
        return this;
    }

    public DiscordEmbed timestampNow() {
        this.timestamp = Instant.now().toString();
        return this;
    }

    public DiscordEmbed footer(String text) {
        this.footerText = text;
        return this;
    }

    public DiscordEmbed footer(String text, String iconUrl) {
        this.footerText = text;
        this.footerIconUrl = iconUrl;
        return this;
    }

    /** Erstellt den vollständigen JSON-Payload für die Discord Webhook API. */
    public String toWebhookPayload() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"embeds\":[{");

        if (title != null) {
            sb.append("\"title\":").append(jsonString(title)).append(",");
        }
        if (description != null) {
            sb.append("\"description\":").append(jsonString(description)).append(",");
        }

        sb.append("\"color\":").append(color).append(",");

        if (!fields.isEmpty()) {
            sb.append("\"fields\":[");
            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i);
                sb.append("{\"name\":").append(jsonString(f.name))
                  .append(",\"value\":").append(jsonString(f.value))
                  .append(",\"inline\":").append(f.inline).append("}");
                if (i < fields.size() - 1) sb.append(",");
            }
            sb.append("],");
        }

        if (timestamp != null) {
            sb.append("\"timestamp\":").append(jsonString(timestamp)).append(",");
        }

        if (footerText != null) {
            sb.append("\"footer\":{\"text\":").append(jsonString(footerText));
            if (footerIconUrl != null) {
                sb.append(",\"icon_url\":").append(jsonString(footerIconUrl));
            }
            sb.append("},");
        }

        // Letztes Komma entfernen
        String result = sb.toString();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }

        return result + "}]}";
    }

    private String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") + "\"";
    }
}
