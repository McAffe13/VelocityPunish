package de.punishsystem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Path dataDirectory;
    private final Logger logger;
    private final Gson gson;

    private DatabaseConfig database;
    private NetworkConfig network;
    private PermissionsConfig permissions;
    private VpnConfig vpn;
    private WebhookConfig webhooks;
    private MessagesConfig messages;
    private Map<String, PunishmentDefinition> punishments;

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public void load() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path configFile = dataDirectory.resolve("config.json");

            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.json")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                        logger.info("Default configuration created.");
                    } else {
                        logger.error("Default configuration could not be loaded from resources!");
                        loadDefaults();
                        return;
                    }
                }
            }

            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                parseConfig(root);
                logger.info("Configuration loaded successfully.");
            }

        } catch (IOException e) {
            logger.error("Error loading configuration!", e);
            loadDefaults();
        }
    }

    private void parseConfig(JsonObject root) {
        database = root.has("database")
                ? gson.fromJson(root.get("database"), DatabaseConfig.class)
                : new DatabaseConfig();

        network = root.has("network")
                ? gson.fromJson(root.get("network"), NetworkConfig.class)
                : new NetworkConfig();

        permissions = root.has("permissions")
                ? gson.fromJson(root.get("permissions"), PermissionsConfig.class)
                : new PermissionsConfig();

        vpn = root.has("vpn")
                ? gson.fromJson(root.get("vpn"), VpnConfig.class)
                : new VpnConfig();

        webhooks = root.has("webhooks")
                ? gson.fromJson(root.get("webhooks"), WebhookConfig.class)
                : new WebhookConfig();

        messages = root.has("messages")
                ? gson.fromJson(root.get("messages"), MessagesConfig.class)
                : new MessagesConfig();

        if (root.has("punishments")) {
            Type mapType = new TypeToken<Map<String, PunishmentDefinition>>() {}.getType();
            punishments = gson.fromJson(root.get("punishments"), mapType);
        } else {
            punishments = new HashMap<>();
        }
    }

    private void loadDefaults() {
        database = new DatabaseConfig();
        network = new NetworkConfig();
        permissions = new PermissionsConfig();
        vpn = new VpnConfig();
        webhooks = new WebhookConfig();
        messages = new MessagesConfig();
        punishments = new HashMap<>();
    }

    public DatabaseConfig getDatabase() { return database; }
    public NetworkConfig getNetwork() { return network; }
    public PermissionsConfig getPermissions() { return permissions; }
    public VpnConfig getVpn() { return vpn; }
    public WebhookConfig getWebhooks() { return webhooks; }
    public MessagesConfig getMessages() { return messages; }

    public Map<String, PunishmentDefinition> getPunishments() {
        return Collections.unmodifiableMap(punishments);
    }

    public PunishmentDefinition getPunishment(String key) {
        return punishments.get(key.toLowerCase());
    }
}
