package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.utils.MessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class KickCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public KickCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getInvalidUsage(),
                    Map.of("usage", "/kick <spieler> <grund>")));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String modName = source instanceof Player p ? p.getUsername() : "Console";

        var onlineOpt = plugin.getServer().getPlayer(targetName);
        if (onlineOpt.isEmpty()) {
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getPlayerNotOnline(),
                    Map.of("player", targetName)));
            return;
        }

        Player target = onlineOpt.get();
        UUID targetUuid = target.getUniqueId();

        plugin.getPunishmentManager().kick(targetUuid, target.getUsername(), modName, reason)
                .thenAccept(kicked -> {
                    if (kicked) {
                        source.sendMessage(MessageUtils.parse(
                                plugin.getConfigManager().getMessages().getKickSuccess(),
                                Map.of("player", target.getUsername())));
                    }
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
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getKick());
    }
}
