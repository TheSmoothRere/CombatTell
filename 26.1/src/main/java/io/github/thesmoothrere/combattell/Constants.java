package io.github.thesmoothrere.combattell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds mod-wide constants used throughout ReLib.
 */
public final class Constants {
    /** The mod identifier used in Fabric and Minecraft namespaces. */
    public static final String MOD_ID = "combattell";
    /** The human-readable display name of the mod. */
    public static final String MOD_NAME = "Combat Tell";
    /** SLF4J logger instance for the mod. */
    public static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    private Constants() {}
}
