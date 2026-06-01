package de.punishsystem.config;

public class MessagesConfig {

    private String banScreen = "§4§lBanned!\n\n§7Reason: §c{reason}\n§7Ban-ID: §e{id}\n\n§7Appeal at: §a{discord_url}\n§7Network: §e{network}";
    private String tempBanScreen = "§6§lTemporarily Banned!\n\n§7Reason: §c{reason}\n§7Duration: §e{duration}\n§7Expires: §e{expire}\n§7Ban-ID: §e{id}\n\n§7Network: §e{network}";
    private String ipBanScreen = "§4§lYour IP is banned!\n\n§7Reason: §c{reason}\n§7Ban-ID: §e{id}\n\n§7Appeal at: §a{discord_url}\n§7Network: §e{network}";
    private String vpnKickScreen = "§4§lVPN / Proxy detected!\n\n§7VPNs and proxies are not allowed on §e{network}§7.\n§7Please disable your VPN and try again.\n\n§7More info: §a{discord_url}";
    private String kickScreen = "§e§lKicked!\n\n§7Reason: §c{reason}\n\n§7Network: §e{network}";
    private String banSuccess = "§aPlayer §e{player} §ahas been permanently banned. §7(ID: §e{id}§7)";
    private String tempBanSuccess = "§aPlayer §e{player} §ahas been banned for §e{duration}§a. §7(ID: §e{id}§7)";
    private String kickSuccess = "§aPlayer §e{player} §ahas been kicked.";
    private String ipBanSuccess = "§aIP §e{ip} §ahas been banned. §7(ID: §e{id}§7)";
    private String unbanSuccess = "§aPlayer §e{player} §ahas been unbanned.";

    private String alreadyBanned = "§cThis player is already banned!";
    private String notBanned = "§cThis player is not banned!";

    private String playerNotFound = "§cPlayer §e{player} §cwas not found!";
    private String playerNotOnline = "§cPlayer §e{player} §cis not online!";
    private String noPermission = "§cYou do not have permission to use this command!";
    private String invalidDuration = "§cInvalid duration! Examples: 30m, 7d, 24h, 1y";
    private String invalidUsage = "§cInvalid usage! §7Use: §e{usage}";
    private String reloadSuccess = "§aConfiguration reloaded successfully!";
    private String reloadError = "§cError while reloading! Check the console.";

    private String historyHeader = "§8§m----------§r §eHistorie: §6{player} §8§m----------";
    private String historyEntry = " §8[§7{index}§8] §c{type} §8| §7{reason} §8| §e{date} §8| §7Mod: §f{mod}";
    private String historyEntryExpired = " §8[§7{index}§8] §8{type} §7{reason} §8| §e{date} §8(expired)";
    private String historyFooter = "§8§m----------§r §7{count} §8entries §8§m----------";
    private String historyEmpty = "§7No punishments found for §e{player}§7.";

    private String checkHeader = "§8§m----------§r §eActive Punishments: §6{player} §8§m----------";
    private String checkBan = " §cBAN §8| §7{reason} §8| §7ID: §e{id} §8| §7Mod: §f{mod}";
    private String checkTempBan = " §6TEMPBAN §8| §7{reason} §8| §7Noch: §e{remaining} §8| §7Mod: §f{mod}";
    private String checkEmpty = "§7No active punishments for §e{player}§7.";

    private String staffNotify = "§8[§6PS§8] §c{player} §7was punished by §c{mod}§7: §c{type} §8(§7{reason}§8)";
    private String punishUnknownKey = "§cUnknown punishment: §e{key}§c! Available: §7{available}";
    private String invalidPunishmentDuration = "§cInvalid duration for punishment §e{key}§c!";
    private String unsupportedPunishmentType = "§cPunishment type §e{type} §cis not supported.";

    private String networkBanScreen = "§4§lNetwork Ban!\n\n§7Reason: §c{reason}\n§7ID: §e{id}\n\n§7Appeal at: §a{discord_url}\n§7Network: §e{network}";
    private String networkBanSuccess = "§aPlayer §e{player} §ahas received a permanent network ban. §7(ID: §e{id}§7)";

    private String multiAccountScreen = "§4§lMulti-Account blocked!\n\n§7Your account is linked to a banned account.\n§7Banned account: §c{banned_player}\n§7Reason: §c{reason}\n\n§7Appeal at: §a{discord_url}\n§7Network: §e{network}";

    private String lookupHeader = "§8§m----------§r §eLookup: §6{player} §8§m----------";
    private String lookupUuid = " §7UUID: §f{uuid}";
    private String lookupStatus = " §7Status: §a{status}";
    private String lookupLastSeen = " §7Last seen: §e{date}";
    private String lookupKnownIps = " §7Known IPs: §f{ips}";
    private String lookupAltAccounts = " §7Linked accounts: §f{accounts}";
    private String lookupAltEntry = "   §8- §f{name}§8: {status}";
    private String lookupAltBanned = "§cBanned §8(§7{reason}§8)";
    private String lookupAltNotBanned = "§aNot banned";
    private String lookupPunishCount = " §7Total punishments: §c{count}";
    private String lookupBanHistoryHeader = " §7Recent bans§8:";
    private String lookupBanEntry = "   §8[§c{type}§8] §7{reason} §8| §e{date} §8| §7Mod: §f{mod}";
    private String lookupBanHistoryEmpty = "   §7No previous bans found.";
    private String lookupActiveBan = " §7Active ban: §c{type} §8| §7{reason} §8| §7Remaining: §e{remaining} §8| §7Mod: §f{mod}";
    private String lookupNoActiveBan = " §7Active ban: §aNone";
    private String lookupFooter = "§8§m--------------------------------------";

    private String warnSuccess = "§aPlayer §e{player} §ahas been warned. §7(ID: §e{id}§7)";
    private String warnMessage = "§6§lWarning!\n\n§7Reason: §c{reason}\n§7Warn-ID: §e{id}\n§7Network: §e{network}";
    private String warningsHeader = "§8§m----------§r §eWarnings: §6{player} §8§m----------";
    private String warningsEntry = " §8[§7{index}§8] §6WARN §8| §7{reason} §8| §e{date} §8| §7Mod: §f{mod} §8| §7ID: §e{id}";
    private String warningsFooter = "§8§m----------§r §7{count} §8warning(s) §8§m----------";
    private String noWarnings = "§7{player} has no active warnings.";
    private String clearWarningsSuccess = "§aCleared §e{count} §awarning(s) for §e{player}§a.";

    public String getBanScreen() { return banScreen; }
    public String getTempBanScreen() { return tempBanScreen; }
    public String getIpBanScreen() { return ipBanScreen; }
    public String getVpnKickScreen() { return vpnKickScreen; }
    public String getKickScreen() { return kickScreen; }
    public String getBanSuccess() { return banSuccess; }
    public String getTempBanSuccess() { return tempBanSuccess; }
    public String getKickSuccess() { return kickSuccess; }
    public String getIpBanSuccess() { return ipBanSuccess; }
    public String getUnbanSuccess() { return unbanSuccess; }
    public String getAlreadyBanned() { return alreadyBanned; }
    public String getNotBanned() { return notBanned; }
    public String getPlayerNotFound() { return playerNotFound; }
    public String getPlayerNotOnline() { return playerNotOnline; }
    public String getNoPermission() { return noPermission; }
    public String getInvalidDuration() { return invalidDuration; }
    public String getInvalidUsage() { return invalidUsage; }
    public String getReloadSuccess() { return reloadSuccess; }
    public String getReloadError() { return reloadError; }
    public String getHistoryHeader() { return historyHeader; }
    public String getHistoryEntry() { return historyEntry; }
    public String getHistoryEntryExpired() { return historyEntryExpired; }
    public String getHistoryFooter() { return historyFooter; }
    public String getHistoryEmpty() { return historyEmpty; }
    public String getCheckHeader() { return checkHeader; }
    public String getCheckBan() { return checkBan; }
    public String getCheckTempBan() { return checkTempBan; }
    public String getCheckEmpty() { return checkEmpty; }
    public String getStaffNotify() { return staffNotify; }
    public String getPunishUnknownKey() { return punishUnknownKey; }
    public String getInvalidPunishmentDuration() { return invalidPunishmentDuration; }
    public String getUnsupportedPunishmentType() { return unsupportedPunishmentType; }

    public String getNetworkBanScreen() { return networkBanScreen; }
    public String getNetworkBanSuccess() { return networkBanSuccess; }
    public String getMultiAccountScreen() { return multiAccountScreen; }

    public String getLookupHeader() { return lookupHeader; }
    public String getLookupUuid() { return lookupUuid; }
    public String getLookupStatus() { return lookupStatus; }
    public String getLookupLastSeen() { return lookupLastSeen; }
    public String getLookupKnownIps() { return lookupKnownIps; }
    public String getLookupAltAccounts() { return lookupAltAccounts; }
    public String getLookupAltEntry() { return lookupAltEntry; }
    public String getLookupAltBanned() { return lookupAltBanned; }
    public String getLookupAltNotBanned() { return lookupAltNotBanned; }
    public String getLookupPunishCount() { return lookupPunishCount; }
    public String getLookupBanHistoryHeader() { return lookupBanHistoryHeader; }
    public String getLookupBanEntry() { return lookupBanEntry; }
    public String getLookupBanHistoryEmpty() { return lookupBanHistoryEmpty; }
    public String getLookupActiveBan() { return lookupActiveBan; }
    public String getLookupNoActiveBan() { return lookupNoActiveBan; }
    public String getLookupFooter() { return lookupFooter; }

    public String getWarnSuccess() { return warnSuccess; }
    public String getWarnMessage() { return warnMessage; }
    public String getWarningsHeader() { return warningsHeader; }
    public String getWarningsEntry() { return warningsEntry; }
    public String getWarningsFooter() { return warningsFooter; }
    public String getNoWarnings() { return noWarnings; }
    public String getClearWarningsSuccess() { return clearWarningsSuccess; }
}
