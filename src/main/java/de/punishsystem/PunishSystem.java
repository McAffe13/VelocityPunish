package de.punishsystem;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.punishsystem.commands.BanCommand;
import de.punishsystem.commands.CheckCommand;
import de.punishsystem.commands.ClearWarningsCommand;
import de.punishsystem.commands.HistoryCommand;
import de.punishsystem.commands.KickCommand;
import de.punishsystem.commands.LookupCommand;
import de.punishsystem.commands.PunishCommand;
import de.punishsystem.commands.PunishReloadCommand;
import de.punishsystem.commands.TempBanCommand;
import de.punishsystem.commands.UnpunishCommand;
import de.punishsystem.commands.WarnCommand;
import de.punishsystem.commands.WarningsCommand;
import de.punishsystem.config.ConfigManager;
import de.punishsystem.database.DatabaseManager;
import de.punishsystem.listeners.LoginListener;
import de.punishsystem.punishment.PunishmentManager;
import de.punishsystem.storage.FileStorageManager;
import de.punishsystem.storage.StorageManager;
import de.punishsystem.vpn.VpnChecker;
import de.punishsystem.webhook.WebhookManager;
import net.labymod.serverapi.server.velocity.LabyModProtocolService;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "punishsystem",
        name = "PunishSystem",
        version = "1.0.0",
        description = "A professional, network-wide punishment system for Velocity proxy servers",
        authors = {"PunishSystem Contributors"}
)
public class PunishSystem {

    // Auf https://bstats.org registrieren und die ID hier eintragen
    private static final int BSTATS_PLUGIN_ID = 31751;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    private ConfigManager configManager;
    private StorageManager storageManager;
    private PunishmentManager punishmentManager;
    private VpnChecker vpnChecker;
    private WebhookManager webhookManager;

    @Inject
    public PunishSystem(ProxyServer server, Logger logger,
                        @DataDirectory Path dataDirectory,
                        Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("╔════════════════════════════════════╗");
        logger.info("║       PunishSystem v1.0.0          ║");
        logger.info("║          by mcaffe13               ║");
        logger.info("╚════════════════════════════════════╝");

        // bStats kann gelegentlich mit HTTP 429 (Rate-Limit) antworten.
        // Die bStats-Library loggt dann meist eine Stacktrace/Warnung.
        // Um den Proxy-Output sauber zu halten, unterdrücken wir (best-effort)
        // das Logging dieser bStats-Pakete während der Initialisierung.
        suppressBStatsLoggingDuring(() -> {
            try {
                metricsFactory.make(this, BSTATS_PLUGIN_ID);
                logger.info("bStats metrics initialized (Plugin-ID: {}).", BSTATS_PLUGIN_ID);
            } catch (Throwable t) {
                // bStats soll den Start nicht blockieren.
                logger.debug("bStats initialization failed (non-critical): {}", t.toString());
            }
        });


        try {
            LabyModProtocolService.initialize(this, this.server, this.logger);

            configManager = new ConfigManager(dataDirectory, logger);
            configManager.load();

            if (configManager.getDatabase().isEnabled()) {
                storageManager = new DatabaseManager(configManager, logger);
                logger.info("Database mode active.");
            } else {
                storageManager = new FileStorageManager(dataDirectory, logger);
                logger.info("File mode active (no database configured).");
            }
            storageManager.initialize();

            webhookManager = new WebhookManager(configManager, logger);
            vpnChecker = new VpnChecker(configManager, logger);

            punishmentManager = new PunishmentManager(
                    this, storageManager, webhookManager, configManager, logger);

            registerListeners();
            registerCommands();

            logger.info("PunishSystem started successfully.");
        } catch (Exception e) {
            logger.error("Critical error while starting PunishSystem!", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (vpnChecker != null) {
            vpnChecker.shutdown();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
        logger.info("PunishSystem has been shut down.");
    }

    private void registerListeners() {
        server.getEventManager().register(this,
                new LoginListener(this, punishmentManager, vpnChecker, configManager));
    }

    private void registerCommands() {
        register("punish", new PunishCommand(this));
        register("ban", new BanCommand(this));
        register("tempban", new TempBanCommand(this), "tban");
        register("kick", new KickCommand(this));
        register("unpunish", new UnpunishCommand(this), "unban");
        register("history", new HistoryCommand(this), "strafen");
        register("check", new CheckCommand(this));
        register("punishreload", new PunishReloadCommand(this), "preload");
        register("lookup", new LookupCommand(this));
        register("warn", new WarnCommand(this));
        register("warnings", new WarningsCommand(this));
        register("clearwarnings", new ClearWarningsCommand(this));

        logger.info("12 commands registered.");
    }

    private void register(String name, com.velocitypowered.api.command.SimpleCommand command, String... aliases) {
        CommandMeta.Builder builder = server.getCommandManager()
                .metaBuilder(name)
                .plugin(this);
        for (String alias : aliases) {
            builder.aliases(alias);
        }
        server.getCommandManager().register(builder.build(), command);
    }

    // ── Getter ────────────────────────────────────────────────────────────────

    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public ConfigManager getConfigManager() { return configManager; }
    public StorageManager getDatabaseManager() { return storageManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public VpnChecker getVpnChecker() { return vpnChecker; }
    public WebhookManager getWebhookManager() { return webhookManager; }

    private void suppressBStatsLoggingDuring(Runnable runnable) {
        // Best-effort: Wenn der Proxy Logback nutzt, können wir für den kurzen Zeitraum
        // das Logging der bStats-Klassen runtersetzen, ohne bStats selbst abzuschalten.
        try {
            Class<?> loggerFactoryClass = Class.forName("ch.qos.logback.classic.LoggerContext");
            // Falls Logback nicht vorhanden ist, Class.forName wirft eine Exception und wir gehen ins fallback.
            Object loggerContext = Class.forName("org.slf4j.LoggerFactory")
                    .getMethod("getILoggerFactory")
                    .invoke(null);

            if (loggerContext != null && loggerContext.getClass().getName().equals("ch.qos.logback.classic.LoggerContext")) {
                Object chLogger = loggerContext.getClass().getMethod("getLogger", String.class)
                        .invoke(loggerContext, "org.bstats");
                Object chLogger2 = loggerContext.getClass().getMethod("getLogger", String.class)
                        .invoke(loggerContext, "com.velocitypowered.proxy.bstats");

                Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
                Object previousLevel1 = null;
                Object previousLevel2 = null;
                try {
                    previousLevel1 = chLogger.getClass().getMethod("getLevel").invoke(chLogger);
                } catch (Throwable ignored) {
                }
                try {
                    previousLevel2 = chLogger2.getClass().getMethod("getLevel").invoke(chLogger2);
                } catch (Throwable ignored) {
                }

                Object errorLevel = levelClass.getField("ERROR").get(null);

                try {
                    if (chLogger != null) {
                        chLogger.getClass().getMethod("setLevel", levelClass).invoke(chLogger, errorLevel);
                    }
                } catch (Throwable ignored) {
                }
                try {
                    if (chLogger2 != null) {
                        chLogger2.getClass().getMethod("setLevel", levelClass).invoke(chLogger2, errorLevel);
                    }
                } catch (Throwable ignored) {
                }

                try {
                    runnable.run();
                } finally {
                    // Restore
                    try {
                        if (chLogger != null && previousLevel1 != null) {
                            chLogger.getClass().getMethod("setLevel", levelClass).invoke(chLogger, previousLevel1);
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        if (chLogger2 != null && previousLevel2 != null) {
                            chLogger2.getClass().getMethod("setLevel", levelClass).invoke(chLogger2, previousLevel2);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                return;
            }
        } catch (Throwable ignored) {
            // Fallback: einfach normal ausführen.
        }

        runnable.run();
    }
}

