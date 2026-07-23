package io.github.thesmoothrere.combattell.util;

import io.github.thesmoothrere.combattell.Constants;

public final class ColorUtils {
    private ColorUtils() {
        /* This utility class should not be instantiated */
    }

    /**
     * Safely parses a primary configuration hex string. If it fails or is invalid,
     * it falls back to parsing a default string value. If BOTH fail, it resorts to a hardcoded fallback.
     */
    public static int parseHexColor(String hexString, String defaultHexStr, int hardcodedFallback) {
        int parsed = parseInternal(hexString);
        if (parsed != -1) {
            return parsed;
        }

        // Primary parsing failed due to a config typo; try parsing the default fallback string option
        int defaultParsed = parseInternal(defaultHexStr);
        if (defaultParsed != -1) {
            return defaultParsed;
        }

        // Everything failed, return the absolute emergency integer fallback (e.g., 0x00FF00)
        return hardcodedFallback;
    }

    private static int parseInternal(String hexString) {
        if (hexString == null || hexString.isBlank()) {
            return -1;
        }

        try {
            String cleaned = hexString.trim();
            if (cleaned.startsWith("#")) {
                cleaned = cleaned.replace("#", "0x");
            } else if (!cleaned.startsWith("0x") && !cleaned.startsWith("0X")) {
                cleaned = "0x" + cleaned;
            }
            return Integer.decode(cleaned);
        } catch (NumberFormatException e) {
            Constants.LOGGER.error("Failed to decode configuration hex string: '{}'", hexString);
            return -1;
        }
    }
}
