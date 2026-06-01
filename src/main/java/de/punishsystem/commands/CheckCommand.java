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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CheckCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public CheckCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/check <spieler>")));
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

            CompletableFuture<Optional<Punishment>> banFuture =
                    plugin.getPunishmentManager().getActiveBan(result.uuid());
            CompletableFuture<List<Punishment>> warnFuture =
                    plugin.getPunishmentManager().getActiveWarnings(result.uuid());

            CompletableFuture.allOf(banFuture, warnFuture).thenRun(() -> {
                MessagesConfig msg = plugin.getConfigManager().getMessages();
                Optional<Punishment> ban = banFuture.join();
                List<Punishment> warnings = warnFuture.join();

                source.sendMessage(MessageUtils.parse(
                        msg.getCheckHeader(), Map.of("player", result.name())));

                if (ban.isPresent() && !ban.get().isExpired()) {
                    Punishment p = ban.get();
                    String template = p.isPermanent() ? msg.getCheckBan() : msg.getCheckTempBan();
                    source.sendMessage(MessageUtils.parse(template, Map.of(
                            "reason", p.getReason(),
                            "id", p.getId(),
                            "mod", p.getModeratorName(),
                            "remaining", TimeParser.formatRemaining(p.getEndTime())
                    )));
                } else if (warnings.isEmpty()) {
                    source.sendMessage(MessageUtils.parse(
                            msg.getCheckEmpty(), Map.of("player", result.name())));
                }

                if (!warnings.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    int i = 1;
                    for (Punishment w : warnings) {
                        source.sendMessage(MessageUtils.parse(msg.getWarningsEntry(), Map.of(
                                "index", String.valueOf(i++),
                                "reason", w.getReason(),
                                "date", sdf.format(new Date(w.getStartTime())),
                                "mod", w.getModeratorName(),
                                "id", w.getId()
                        )));
                    }
                }
            }).exceptionally(ex -> {
                plugin.getLogger().error("Error checking punishments for {}", targetName, ex);
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
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getCheck());
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
