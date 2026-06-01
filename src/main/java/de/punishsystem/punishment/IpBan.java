package de.punishsystem.punishment;

import java.util.UUID;

public class IpBan {

    private String id;
    private String ipAddress;
    private UUID playerUuid;
    private String playerName;
    private String moderatorName;
    private String reason;
    private long startTime;
    private long endTime;
    private boolean active;

    public IpBan() {}

    public IpBan(String id, String ipAddress, UUID playerUuid, String playerName,
                 String moderatorName, String reason,
                 long startTime, long endTime, boolean active) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.moderatorName = moderatorName;
        this.reason = reason;
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getModeratorName() { return moderatorName; }
    public void setModeratorName(String moderatorName) { this.moderatorName = moderatorName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
