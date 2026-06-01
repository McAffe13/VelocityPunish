package de.punishsystem.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.punishsystem.PunishSystem;
import de.punishsystem.utils.MessageUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PunishReloadCommand implements SimpleCommand {

    private final PunishSystem plugin;

    public PunishReloadCommand(PunishSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        try {
            plugin.getConfigManager().load();
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getReloadSuccess()));
            plugin.getLogger().info("Configuration reloaded by: {}",
                    source instanceof com.velocitypowered.api.proxy.Player p
                            ? p.getUsername() : "Console");
        } catch (Exception e) {
            plugin.getLogger().error("Error reloading configuration!", e);
            source.sendMessage(MessageUtils.parse(
                    plugin.getConfigManager().getMessages().getReloadError()));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(plugin.getConfigManager().getPermissions().getReload());
    }
}
