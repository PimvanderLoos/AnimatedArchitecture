package nl.pim16aap2.animatedarchitecture.spigot.core.util;

import lombok.extern.flogger.Flogger;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/**
 * List of supported Minecraft versions.
 * <p>
 * Unsupported versions may work fine, but some features may be degraded (e.g. Materials support).
 * <p>
 * When a version is not supported, the used version will default to the most recent version.
 */
@Flogger
@SuppressWarnings("PMD.FieldNamingConventions")
public enum MinecraftVersion
{
    v1_19_R3,
    v1_20_R1,
    ;

    private static final MinecraftVersion LATEST_VERSION;
    private static final ParsedVersion PARSED_VERSION;

    static
    {
        final MinecraftVersion[] values = MinecraftVersion.values();
        LATEST_VERSION = values[values.length - 1];
        PARSED_VERSION = parseCurrentVersion();
    }

    /**
     * Getter for the used version.
     * <p>
     * This may not necessarily be the actual version of Minecraft that the server is running.
     * <p>
     * See {@link #isSupportedVersion()} for more information.
     *
     * @return The currently used version.
     */
    public static MinecraftVersion getUsedVersion()
    {
        return PARSED_VERSION.version();
    }

    /**
     * Checks if the currently parsed version if fully supported.
     * <p>
     * When the server is running a version that is not listed in this enum, the most recent version will be used, but
     * this method will return false to indicate that it is not fully supported.
     *
     * @return True if this currently used version is fully supported.
     */
    public static boolean isSupportedVersion()
    {
        return PARSED_VERSION.fullSupport();
    }

    private static ParsedVersion parseCurrentVersion()
    {
        @Nullable String input = null;
        try
        {
            input = Bukkit.getServer().getClass().getPackage().getName();
            return new ParsedVersion(MinecraftVersion.valueOf(input.replace(".", ",").split(",")[3]), true);
        }
        catch (final ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            log.atInfo().log(
                "Failed to parse Minecraft version: '%s'. Defaulting to %s.", input, LATEST_VERSION.name());
            return new ParsedVersion(LATEST_VERSION, false);
        }
    }

    /**
     * Represents a parsed version of Minecraft and its status.
     *
     * @param version
     *     The version that was parsed.
     * @param fullSupport
     *     True if this version is fully supported.
     */
    private record ParsedVersion(MinecraftVersion version, boolean fullSupport)
    {}
}
