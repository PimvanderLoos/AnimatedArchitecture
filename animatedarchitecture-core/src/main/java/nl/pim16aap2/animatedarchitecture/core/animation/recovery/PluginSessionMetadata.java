package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

/**
 * Metadata captured when an AnimatedArchitecture plugin session starts.
 *
 * @param pluginVersion
 *     The AnimatedArchitecture plugin version.
 * @param serverVersion
 *     The Bukkit server version.
 * @param minecraftVersion
 *     The Minecraft version exposed by the server.
 * @param serverSoftware
 *     The server software name.
 */
public record PluginSessionMetadata(
    String pluginVersion,
    String serverVersion,
    String minecraftVersion,
    String serverSoftware
)
{
}
