package de.punishsystem.utils;

public final class TimeParser {

    private TimeParser() {}

    /**
     * Parses a duration string like "30d", "7h", "2h30m", "1y" into milliseconds.
     * Returns -1 for permanent/invalid input.
     */
    public static long parse(String input) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("permanent")
                || input.equalsIgnoreCase("perm") || input.equals("-1")) {
            return -1L;
        }

        long total = 0L;
        StringBuilder num = new StringBuilder();

        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.isEmpty()) continue;
                long value = Long.parseLong(num.toString());
                num = new StringBuilder();

                total += switch (c) {
                    case 's' -> value * 1_000L;
                    case 'm' -> value * 60_000L;
                    case 'h' -> value * 3_600_000L;
                    case 'd' -> value * 86_400_000L;
                    case 'w' -> value * 604_800_000L;
                    case 'y' -> value * 31_536_000_000L;
                    default -> 0L;
                };
            }
        }

        return total > 0 ? total : -1L;
    }

    /** Formats milliseconds into a human-readable duration string. */
    public static String format(long milliseconds) {
        if (milliseconds <= 0) {
            return "Permanent";
        }

        long totalSeconds = milliseconds / 1000;

        long years = totalSeconds / 31_536_000;
        totalSeconds %= 31_536_000;
        long weeks = totalSeconds / 604_800;
        totalSeconds %= 604_800;
        long days = totalSeconds / 86_400;
        totalSeconds %= 86_400;
        long hours = totalSeconds / 3_600;
        totalSeconds %= 3_600;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (years > 0)   sb.append(years).append(years == 1 ? " year " : " years ");
        if (weeks > 0)   sb.append(weeks).append(weeks == 1 ? " week " : " weeks ");
        if (days > 0)    sb.append(days).append(days == 1 ? " day " : " days ");
        if (hours > 0)   sb.append(hours).append(hours == 1 ? " hour " : " hours ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        if (sb.isEmpty() || seconds > 0) {
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString().trim();
    }

    /** Formats the remaining time until a Unix timestamp in milliseconds. */
    public static String formatRemaining(long endTimeMs) {
        long remaining = endTimeMs - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        return format(remaining);
    }

    /** Returns true if the input is a valid duration string. */
    public static boolean isValid(String input) {
        if (input == null || input.isBlank()) return false;
        if (input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perm")) return true;
        return parse(input) > 0;
    }
}
