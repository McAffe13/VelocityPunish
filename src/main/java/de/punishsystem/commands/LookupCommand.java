package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.config.MessagesConfig;
import de.punishsystem.punishment.Punishment;
import de.punishsystem.utils.MessageUtils;
import de.punishsystem.utils.MojangApi;
import de.punishsystem.utils.TimeParser;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LookupCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public LookupCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/lookup <player>")));
            return;
        }

        String targetName = args[0];

        resolvePlayer(targetName).thenAccept(result -> {
            if (result == null) {
                source.sendMessage(MessageUtils.parse(
                        plugin.getConfigManager().getMessages().getPlayerNotFound(),
                        Map.of("player", targetName)));
                return;
            }

            MessagesConfig msg = plugin.getConfigManager().getMessages();

            CompletableFuture<Optional<Punishment>> banFuture =
                    plugin.getPunishmentManager().getActiveBan(result.uuid());
            CompletableFuture<List<String>> ipsFuture =
                    plugin.getDatabaseManager().getPlayerIps(result.uuid());
            CompletableFuture<List<String[]>> altsFuture =
                    plugin.getDatabaseManager().getLinkedAccounts(result.uuid());
            CompletableFuture<Integer> countFuture =
                    plugin.getDatabaseManager().getPunishmentCount(result.uuid());
            CompletableFuture<Optional<Long>> lastSeenFuture =
                    plugin.getDatabaseManager().getPlayerLastSeen(result.uuid());
            CompletableFuture<List<Punishment>> historyFuture =
                    plugin.getDatabaseManager().getPlayerHistory(result.uuid());

            CompletableFuture.allOf(banFuture, ipsFuture, altsFuture, countFuture, lastSeenFuture, historyFuture)
                    .thenCompose(v -> {
                        List<String[]> alts = altsFuture.join();
                        // Fetch ban status for each linked account
                        List<CompletableFuture<Object[]>> altBanFutures = alts.stream()
                                .map(alt -> plugin.getPunishmentManager()
                                        .getActiveBan(UUID.fromString(alt[0]))
                                        .thenApply(ban -> new Object[]{alt[0], alt[1], ban}))
                                .collect(Collectors.toList());
                        return CompletableFuture.allOf(altBanFutures.toArray(new CompletableFuture[0]))
                                .thenAccept(ignored -> {
                                    List<Object[]> altBans = altBanFutures.stream()
                                            .map(CompletableFuture::join)
                                            .collect(Collectors.toList());
                                    displayLookup(source, msg, result,
                                            banFuture.join(), ipsFuture.join(),
                                            altBans, countFuture.join(),
                                            lastSeenFuture.join(), historyFuture.join());
                                });
                    })
                    .exceptionally(ex -> {
                        plugin.getLogger().error("Error executing /lookup for {}", targetName, ex);
                        return null;
                    });
        }).exceptionally(ex -> {
            plugin.getLogger().error("Error resolving player {}", targetName, ex);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void displayLookup(CommandSource source, MessagesConfig msg, PlayerRef result,
                                Optional<Punishment> ban, List<String> ips,
                                List<Object[]> altBans, int punishCount,
                                Optional<Long> lastSeen, List<Punishment> history) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        // Online-Status
        Optional<Player> onlinePlayer = plugin.getServer().getPlayer(result.uuid());
        String statusStr;
        if (onlinePlayer.isPresent()) {
            String serverName = onlinePlayer.get().getCurrentServer()
                    .map(s -> s.getServerInfo().getName()).orElse("?");
            statusStr = "§aOnline §7(§f" + serverName + "§7)";
        } else {
            statusStr = "§cOffline";
        }

        source.sendMessage(MessageUtils.parse(
                msg.getLookupHeader(), Map.of("player", result.name())));
        source.sendMessage(MessageUtils.parse(
                msg.getLookupUuid(), Map.of("uuid", result.uuid().toString())));
        source.sendMessage(MessageUtils.parse(
                msg.getLookupStatus(), Map.of("status", statusStr)));

        // Last seen (only when offline)
        if (onlinePlayer.isEmpty()) {
            String dateStr = lastSeen.map(ts -> sdf.format(new Date(ts))).orElse("Unknown");
            source.sendMessage(MessageUtils.parse(
                    msg.getLookupLastSeen(), Map.of("date", dateStr)));
        }

        // Known IPs
        if (plugin.getConfigManager().getNetwork().isShowIpsInLookup()) {
            String ipsStr = ips.isEmpty() ? "§7None" : String.join("§7, §f", ips);
            source.sendMessage(MessageUtils.parse(
                    msg.getLookupKnownIps(), Map.of("ips", ipsStr)));
        }

        // Linked accounts with ban status
        if (altBans.isEmpty()) {
            source.sendMessage(MessageUtils.parse(
                    msg.getLookupAltAccounts(), Map.of("accounts", "§7None")));
        } else {
            source.sendMessage(MessageUtils.parse(
                    msg.getLookupAltAccounts(),
                    Map.of("accounts", "§8(" + altBans.size() + ")")));
            for (Object[] altData : altBans) {
                String altName = (String) altData[1];
                Optional<Punishment> altBan = (Optional<Punishment>) altData[2];
                String altStatus;
                if (altBan.isPresent() && !altBan.get().isExpired()) {
                    altStatus = MessageUtils.replace(
                            msg.getLookupAltBanned(),
                            Map.of("reason", altBan.get().getReason()));
                } else {
                    altStatus = msg.getLookupAltNotBanned();
                }
                source.sendMessage(MessageUtils.parse(
                        msg.getLookupAltEntry(), Map.of("name", altName, "status", altStatus)));
            }
        }

        // Punishment count
        source.sendMessage(MessageUtils.parse(
                msg.getLookupPunishCount(), Map.of("count", String.valueOf(punishCount))));

        // Recent bans (max 5, BAN/TEMPBAN/NETWORK_BAN only)
        List<Punishment> recentBans = history.stream()
                .filter(p -> {
                    String t = p.getType().name();
                    return t.equals("BAN") || t.equals("TEMPBAN") || t.equals("NETWORK_BAN");
                })
                .limit(5)
                .collect(Collectors.toList());

        source.sendMessage(MessageUtils.parse(msg.getLookupBanHistoryHeader(), Map.of()));
        if (recentBans.isEmpty()) {
            source.sendMessage(MessageUtils.parse(msg.getLookupBanHistoryEmpty(), Map.of()));
        } else {
            for (Punishment p : recentBans) {
                source.sendMessage(MessageUtils.parse(
                        msg.getLookupBanEntry(), Map.of(
                                "type", p.getType().getDisplayName(),
                                "reason", p.getReason(),
                                "date", sdf.format(new Date(p.getStartTime())),
                                "mod", p.getModeratorName()
                        )));
            }
        }

        // Active ban
        if (ban.isPresent() && !ban.get().isExpired()) {
            Punishment p = ban.get();
            String remaining = p.isPermanent()
                    ? "§4Permanent"
                    : "§e" + TimeParser.formatRemaining(p.getEndTime());
            source.sendMessage(MessageUtils.parse(
                    msg.getLookupActiveBan(), Map.of(
                            "type", p.getType().getDisplayName(),
                            "reason", p.getReason(),
                            "mod", p.getModeratorName(),
                            "remaining", remaining
                    )));
        } else {
            source.sendMessage(MessageUtils.parse(msg.getLookupNoActiveBan(), Map.of()));
        }

        source.sendMessage(MessageUtils.parse(msg.getLookupFooter(), Map.of()));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            String partial = invocation.arguments().length == 0 ? "" : invocation.arguments()[0].toLowerCase();
            return CompletableFuture.completedFuture(
                    plugin.getServer().getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList()));
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getLookup());
    }

    private CompletableFuture<PlayerRef> resolvePlayer(String name) {
        var online = plugin.getServer().getPlayer(name);
        if (online.isPresent()) {
            return CompletableFuture.completedFuture(
                    new PlayerRef(online.get().getUniqueId(), online.get().getUsername()));
        }
        return plugin.getDatabaseManager().getPlayerUuidByName(name).thenCompose(opt -> {
            if (opt.isPresent()) return CompletableFuture.completedFuture(new PlayerRef(opt.get(), name));
            return MojangApi.fetchUuid(name).thenApply(u -> u.map(id -> new PlayerRef(id, name)).orElse(null));
        });
    }

    record PlayerRef(UUID uuid, String name) {}
}
