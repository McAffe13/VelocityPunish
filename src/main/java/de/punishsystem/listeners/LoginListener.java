package de.punishsystem.listeners;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.punishsystem.PunishSystem;
import de.punishsystem.config.ConfigManager;
import de.punishsystem.punishment.IpBan;
import de.punishsystem.punishment.Punishment;
import de.punishsystem.punishment.PunishmentManager;
import de.punishsystem.vpn.VpnChecker;
import de.punishsystem.vpn.VpnResult;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LoginListener {

    private final PunishSystem plugin;
    private final PunishmentManager punishments;
    private final VpnChecker vpnChecker;
    private final ConfigManager config;
    private final Logger logger;

    public LoginListener(PunishSystem plugin, PunishmentManager punishments,
                         VpnChecker vpnChecker, ConfigManager config) {
        this.plugin = plugin;
        this.punishments = punishments;
        this.vpnChecker = vpnChecker;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    @Subscribe
    public void onLogin(LoginEvent event, Continuation continuation) {
        Player player = event.getPlayer();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        performChecks(event, player, ip)
                .exceptionally(ex -> {
                    logger.error("Error during login check for {}: {}", player.getUsername(), ex.getMessage());
                    return null;
                })
                .whenComplete((v, ex) -> continuation.resume());
    }

    private CompletableFuture<Void> performChecks(LoginEvent event, Player player, String ip) {
        // Always save IP for multi-account tracking
        plugin.getDatabaseManager().savePlayerIp(player.getUniqueId(), player.getUsername(), ip);

        // 1. UUID-Ban prüfen
        return punishments.getActiveBan(player.getUniqueId())
                .thenCompose(banOpt -> {
                    if (banOpt.isPresent()) {
                        Punishment ban = banOpt.get();
                        if (ban.isExpired()) {
                            plugin.getDatabaseManager().deactivatePunishment(ban.getId());
                        } else {
                            Component screen = buildBanScreen(ban);
                            event.setResult(ResultedEvent.ComponentResult.denied(screen));
                            return CompletableFuture.completedFuture(Boolean.TRUE);
                        }
                    }
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                })
                // 2. Check IP-ban (only if not already blocked)
                .thenCompose(denied -> {
                    if (Boolean.TRUE.equals(denied)) {
                        return CompletableFuture.completedFuture(Boolean.TRUE);
                    }
                    return punishments.getActiveIpBan(ip).thenApply(ipBanOpt -> {
                        if (ipBanOpt.isPresent()) {
                            IpBan ban = ipBanOpt.get();
                            if (ban.isExpired()) {
                                plugin.getDatabaseManager().deactivateIpBan(ban.getId());
                                return Boolean.FALSE;
                            }
                            event.setResult(ResultedEvent.ComponentResult.denied(
                                    punishments.buildIpBanScreen(ban)));
                            return Boolean.TRUE;
                        }
                        return Boolean.FALSE;
                    });
                })
                // 3. Check multi-account (all known IPs, only if not already blocked)
                .thenCompose(denied -> {
                    if (Boolean.TRUE.equals(denied)) {
                        return CompletableFuture.completedFuture(Boolean.TRUE);
                    }
                    return plugin.getDatabaseManager()
                            .getBannedAccountForPlayer(player.getUniqueId())
                            .thenApply(bannedOpt -> {
                                if (bannedOpt.isPresent()) {
                                    Punishment banned = bannedOpt.get();
                                    event.setResult(ResultedEvent.ComponentResult.denied(
                                            punishments.buildMultiAccountScreen(banned)));
                                    logger.info("[MULTI-ACCOUNT] {} ({}) blocked — linked to banned account {} ({})",
                                            player.getUsername(), ip, banned.getPlayerName(), banned.getId());
                                    return Boolean.TRUE;
                                }
                                return Boolean.FALSE;
                            });
                })
                // 4. Check VPN (only if not already blocked and VPN check is enabled)
                .thenCompose(denied -> {
                    if (Boolean.TRUE.equals(denied)) {
                        return CompletableFuture.completedFuture((Void) null);
                    }
                    if (!config.getVpn().isEnabled()) {
                        return CompletableFuture.completedFuture((Void) null);
                    }
                    if (player.hasPermission(config.getPermissions().getVpnBypass())) {
                        return CompletableFuture.completedFuture((Void) null);
                    }

                    return vpnChecker.check(ip).thenAccept(result -> {
                        if (result.isVpn() && config.getVpn().isKickOnDetect()) {
                            event.setResult(ResultedEvent.ComponentResult.denied(
                                    punishments.buildVpnKickScreen()));

                            plugin.getDatabaseManager().saveVpnLog(
                                    player.getUniqueId().toString(),
                                    player.getUsername(),
                                    ip,
                                    result.getIsp(),
                                    result.getCountryCode()
                            );

                            plugin.getWebhookManager().sendVpnWebhook(
                                    player.getUsername(),
                                    player.getUniqueId().toString(),
                                    ip,
                                    result.getIsp(),
                                    result.getCountryCode()
                            );

                            logger.info("[VPN] {} ({}) blocked — ISP: {}, Country: {}",
                                    player.getUsername(), ip, result.getIsp(), result.getCountryCode());
                        }
                    });
                });
    }

    private Component buildBanScreen(Punishment ban) {
        if (ban.getType() == de.punishsystem.punishment.PunishmentType.NETWORK_BAN) {
            return punishments.buildNetworkBanScreen(ban);
        }
        return ban.isPermanent() ? punishments.buildBanScreen(ban) : punishments.buildTempBanScreen(ban);
    }
}
