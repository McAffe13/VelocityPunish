package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.config.PunishmentDefinition;
import de.punishsystem.punishment.PunishmentType;
import de.punishsystem.utils.MessageUtils;
import de.punishsystem.utils.MojangApi;
import de.punishsystem.utils.TimeParser;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PunishCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public PunishCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/punish <player> <key>")));
            return;
        }

        String targetName = args[0];
        String punishKey = args[1].toLowerCase();
        String modName = source instanceof Player p ? p.getUsername() : "Console";
        UUID modUuid = source instanceof Player p ? p.getUniqueId() : null;

        PunishmentDefinition definition = plugin.getConfigManager().getPunishment(punishKey);
        if (definition == null) {
            String available = String.join(", ", plugin.getConfigManager().getPunishments().keySet());
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getPunishUnknownKey(),
                    Map.of("key", punishKey, "available", available)));
            return;
        }

        resolvePlayer(targetName).thenAccept(result -> {
            if (result == null) {
                source.sendMessage(MessageUtils.parse(
                        plugin.getConfigManager().getMessages().getPlayerNotFound(),
                        Map.of("player", targetName)));
                return;
            }

            PunishmentType type = definition.getPunishmentType();
            String reason = definition.getReason();
            long durationMs = definition.getDuration() != null
                    ? TimeParser.parse(definition.getDuration())
                    : -1L;

            switch (type) {
                case BAN -> plugin.getPunishmentManager()
                        .ban(result.name(), result.uuid(), modName, modUuid, reason)
                        .thenAccept(ban -> source.sendMessage(MessageUtils.parse(
                                plugin.getConfigManager().getMessages().getBanSuccess(),
                                Map.of("player", result.name(), "id", ban.getId()))));

                case TEMPBAN -> {
                    if (durationMs <= 0) {
                        source.sendMessage(MessageUtils.parse(
                                plugin.getConfigManager().getMessages().getInvalidPunishmentDuration(),
                                Map.of("key", punishKey)));
                        return;
                    }
                    plugin.getPunishmentManager()
                            .tempBan(result.name(), result.uuid(), modName, modUuid, reason, durationMs)
                            .thenAccept(ban -> source.sendMessage(MessageUtils.parse(
                                    plugin.getConfigManager().getMessages().getTempBanSuccess(),
                                    Map.of("player", result.name(),
                                            "duration", TimeParser.format(durationMs),
                                            "id", ban.getId()))));
                }

                case KICK -> {
                    var onlineOpt = plugin.getServer().getPlayer(result.uuid());
                    if (onlineOpt.isEmpty()) {
                        source.sendMessage(MessageUtils.parse(
                                plugin.getConfigManager().getMessages().getPlayerNotOnline(),
                                Map.of("player", result.name())));
                        return;
                    }
                    plugin.getPunishmentManager()
                            .kick(result.uuid(), result.name(), modName, reason)
                            .thenAccept(kicked -> {
                                if (kicked) {
                                    source.sendMessage(MessageUtils.parse(
                                            plugin.getConfigManager().getMessages().getKickSuccess(),
                                            Map.of("player", result.name())));
                                }
                            });
                }

                case NETWORK_BAN -> plugin.getPunishmentManager()
                        .networkBan(result.name(), result.uuid(), modName, modUuid, reason)
                        .thenAccept(p -> source.sendMessage(MessageUtils.parse(
                                plugin.getConfigManager().getMessages().getNetworkBanSuccess(),
                                Map.of("player", result.name(), "id", p.getId()))));

                default -> source.sendMessage(MessageUtils.parse(
                        plugin.getConfigManager().getMessages().getUnsupportedPunishmentType(),
                        Map.of("type", type.name())));
            }
        }).exceptionally(ex -> {
            plugin.getLogger().error("Error executing /punish for {}", targetName, ex);
            return null;
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
            String partial = args[1].toLowerCase();
            return CompletableFuture.completedFuture(
                    plugin.getConfigManager().getPunishments().keySet().stream()
                            .filter(k -> k.startsWith(partial))
                            .collect(Collectors.toList()));
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getPunish());
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
