package io.github.thesmoothrere.combattell.util;

public final class TimeUtils {
    private TimeUtils() {
        /* This utility class should not be instantiated */
    }

    /**
     * Safely converts a user-friendly decimal second value into Minecraft game ticks.
     * E.g., 1.5 seconds -> 30 ticks.
     *
     * @param seconds The duration in seconds from the config field.
     * @param defaultSeconds The fallback value if the configuration value is invalid.
     * @return The calculated game tick count as an integer layout value.
     */
    public static int secondsToTicks(double seconds, double defaultSeconds) {
        // Boundary Safety: Ensure the configuration value is positive and reasonable
        if (seconds <= 0.0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            seconds = defaultSeconds;
        }

        // Convert to ticks: 20 ticks per second
        double calculatedTicks = seconds * 20.0;

        // Round to the nearest complete tick frame and clamp to a minimum of 1 tick
        return Math.max(1, (int) Math.round(calculatedTicks));
    }
}
