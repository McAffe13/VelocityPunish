package de.punishsystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

public final class MessageUtils {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private MessageUtils() {}

    /** Konvertiert einen §-formatierten String in ein Adventure-Component. */
    public static Component parse(String message) {
        return LEGACY.deserialize(message);
    }

    /** Ersetzt Platzhalter und konvertiert in Component. */
    public static Component parse(String message, Map<String, String> placeholders) {
        return LEGACY.deserialize(replace(message, placeholders));
    }

    /** Ersetzt Platzhalter in einem String. */
    public static String replace(String message, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    /** Erstellt ein Component mit dem Netzwerk-Prefix. */
    public static Component withPrefix(String prefix, String message) {
        return LEGACY.deserialize(prefix + message);
    }

    /** Entfernt alle § Farbcodes aus einem String. */
    public static String stripColor(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }
}
