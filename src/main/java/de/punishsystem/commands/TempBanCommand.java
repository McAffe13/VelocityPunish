package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.utils.MessageUtils;
import de.punishsystem.utils.MojangApi;
import de.punishsystem.utils.TimeParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TempBanCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public TempBanCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 3) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/tempban <player> <duration> <reason>")));
            return;
        }

        String targetName = args[0];
        String durationStr = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String modName = source instanceof Player p ? p.getUsername() : "Console";
        UUID modUuid = source instanceof Player p ? p.getUniqueId() : null;

        long durationMs = TimeParser.parse(durationStr);
        if (durationMs <= 0) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidDuration()));
            return;
        }

        resolvePlayer(targetName).thenAccept(result -> {
            if (result == null) {
                source.sendMessage(MessageUtils.parse(
                        plugin.getConfigManager().getMessages().getPlayerNotFound(),
                        Map.of("player", targetName)));
                return;
            }

            plugin.getPunishmentManager().getActiveBan(result.uuid()).thenCompose(existing -> {
                if (existing.isPresent() && !existing.get().isExpired()) {
                    source.sendMessage(MessageUtils.parse(
                            plugin.getConfigManager().getMessages().getAlreadyBanned()));
                    return CompletableFuture.completedFuture(null);
                }
                return plugin.getPunishmentManager()
                        .tempBan(result.name(), result.uuid(), modName, modUuid, reason, durationMs)
                        .thenAccept(ban -> source.sendMessage(MessageUtils.parse(
                                plugin.getConfigManager().getMessages().getTempBanSuccess(),
                                Map.of("player", result.name(),
                                        "duration", TimeParser.format(durationMs),
                                        "id", ban.getId()))));
            }).exceptionally(ex -> {
                plugin.getLogger().error("Error temp-banning {}", targetName, ex);
                return null;
            });
        });
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            return CompletableFuture.completedFuture(
                    plugin.getServer().getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList()));
        }
        if (args.length == 2) {
            return CompletableFuture.completedFuture(
                    List.of("30m", "1h", "1d", "7d", "14d", "30d", "1y"));
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getTempban());
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
