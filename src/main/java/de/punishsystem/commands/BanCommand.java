package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.utils.MessageUtils;
import de.punishsystem.utils.MojangApi;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BanCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public BanCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/ban <player> <reason>")));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String modName = source instanceof Player p ? p.getUsername() : "Console";
        UUID modUuid = source instanceof Player p ? p.getUniqueId() : null;

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
                        .ban(result.name(), result.uuid(), modName, modUuid, reason)
                        .thenAccept(ban -> source.sendMessage(MessageUtils.parse(
                                plugin.getConfigManager().getMessages().getBanSuccess(),
                                Map.of("player", result.name(), "id", ban.getId()))));
            }).exceptionally(ex -> {
                plugin.getLogger().error("Error banning {}", targetName, ex);
                return null;
            });
        });
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
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getBan());
    }

    private CompletableFuture<PlayerRef> resolvePlayer(String name) {
        var online = plugin.getServer().getPlayer(name);
        if (online.isPresent()) {
            return CompletableFuture.completedFuture(
                    new PlayerRef(online.get().getUniqueId(), online.get().getUsername()));
        }
        return plugin.getDatabaseManager().getPlayerUuidByName(name).thenCompose(opt -> {
            if (opt.isPresent()) {
                return CompletableFuture.completedFuture(new PlayerRef(opt.get(), name));
            }
            return MojangApi.fetchUuid(name).thenApply(uuidOpt ->
                    uuidOpt.map(u -> new PlayerRef(u, name)).orElse(null));
        });
    }

    record PlayerRef(UUID uuid, String name) {}
}
