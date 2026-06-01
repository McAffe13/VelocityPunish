package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.utils.MessageUtils;
import de.punishsystem.utils.MojangApi;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WarnCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public WarnCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/warn <player> <reason>")));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String modName = source instanceof Player p ? p.getUsername() : "Console";
        UUID modUuid = source instanceof Player p ? p.getUniqueId() : new UUID(0, 0);

        resolvePlayer(targetName).thenAccept(result -> {
            if (result == null) {
                source.sendMessage(MessageUtils.parse(
                        plugin.getConfigManager().getMessages().getPlayerNotFound(),
                        Map.of("player", targetName)));
                return;
            }

            plugin.getPunishmentManager()
                    .warn(result.name(), result.uuid(), modName, modUuid, reason)
                    .thenAccept(p -> source.sendMessage(MessageUtils.parse(
                            plugin.getConfigManager().getMessages().getWarnSuccess(),
                            Map.of("player", result.name(), "id", p.getId()))))
                    .exceptionally(ex -> {
                        plugin.getLogger().error("Error warning player {}", targetName, ex);
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
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getWarn());
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
