package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.config.MessagesConfig;
import de.punishsystem.punishment.Punishment;
import de.punishsystem.utils.MessageUtils;
import de.punishsystem.utils.MojangApi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HistoryCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public HistoryCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/history <player>")));
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

            plugin.getDatabaseManager().getPlayerHistory(result.uuid()).thenAccept(history -> {
                MessagesConfig msg = plugin.getConfigManager().getMessages();

                if (history.isEmpty()) {
                    source.sendMessage(MessageUtils.parse(
                            msg.getHistoryEmpty(), Map.of("player", result.name())));
                    return;
                }

                source.sendMessage(MessageUtils.parse(
                        msg.getHistoryHeader(), Map.of("player", result.name())));

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                int index = 1;
                for (Punishment p : history) {
                    String date = sdf.format(new Date(p.getStartTime()));
                    String template = (p.isActive() && !p.isExpired())
                            ? msg.getHistoryEntry()
                            : msg.getHistoryEntryExpired();

                    source.sendMessage(MessageUtils.parse(template, Map.of(
                            "index", String.valueOf(index++),
                            "type", p.getType().getDisplayName(),
                            "reason", p.getReason(),
                            "date", date,
                            "mod", p.getModeratorName()
                    )));
                }

                source.sendMessage(MessageUtils.parse(
                        msg.getHistoryFooter(), Map.of("count", String.valueOf(history.size()))));

            }).exceptionally(ex -> {
                plugin.getLogger().error("Error loading history for {}", targetName, ex);
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
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getHistory());
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
