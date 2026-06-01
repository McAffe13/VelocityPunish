package de.punishsystem.punishment;

import java.util.UUID;

public class Punishment {

    private String id;
    private UUID playerUuid;
    private String playerName;
    private UUID moderatorUuid;
    private String moderatorName;
    private PunishmentType type;
    private String reason;
    private long duration;
    private long startTime;
    private long endTime;
    private boolean active;

    public Punishment() {}

    public Punishment(String id, UUID playerUuid, String playerName,
                      UUID moderatorUuid, String moderatorName,
                      PunishmentType type, String reason,
                      long duration, long startTime, long endTime,
                      boolean active) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.moderatorUuid = moderatorUuid;
        this.moderatorName = moderatorName;
        this.type = type;
        this.reason = reason;
        this.duration = duration;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
    }

    public boolean isExpired() {
        return endTime > 0 && System.currentTimeMillis() > endTime;
    }

    public boolean isPermanent() {
        return endTime <= 0;
    }

    public long getRemainingMs() {
        if (isPermanent()) return -1L;
        return Math.max(0L, endTime - System.currentTimeMillis());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public UUID getModeratorUuid() { return moderatorUuid; }
    public void setModeratorUuid(UUID moderatorUuid) { this.moderatorUuid = moderatorUuid; }

    public String getModeratorName() { return moderatorName; }
    public void setModeratorName(String moderatorName) { this.moderatorName = moderatorName; }

    public PunishmentType getType() { return type; }
    public void setType(PunishmentType type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
