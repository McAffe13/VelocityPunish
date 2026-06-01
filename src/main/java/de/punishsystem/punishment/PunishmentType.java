package de.punishsystem.punishment;

public enum PunishmentType {
    BAN,
    TEMPBAN,
    KICK,
    WARN,
    IPBAN,
    NETWORK_BAN;

    public boolean isBan() {
        return this == BAN || this == TEMPBAN || this == IPBAN || this == NETWORK_BAN;
    }

    public boolean isTemporary() {
        return this == TEMPBAN;
    }

    public String getDisplayName() {
        return switch (this) {
            case BAN -> "Ban";
            case TEMPBAN -> "TempBan";
            case KICK -> "Kick";
            case WARN -> "Warn";
            case IPBAN -> "IP-Ban";
            case NETWORK_BAN -> "Network Ban";
        };
    }
}
