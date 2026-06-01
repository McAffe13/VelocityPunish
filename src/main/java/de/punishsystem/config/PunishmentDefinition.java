package de.punishsystem.config;

import de.punishsystem.punishment.PunishmentType;

public class PunishmentDefinition {

    private String type;
    private String duration;
    private String reason;

    public PunishmentType getPunishmentType() {
        if (type == null) return PunishmentType.KICK;
        try {
            return PunishmentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PunishmentType.KICK;
        }
    }

    public String getType() { return type; }
    public String getDuration() { return duration; }
    public String getReason() { return reason; }
}
