package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable data for an AnimatedArchitecture plugin lifecycle session.
 *
 * @param uuid
 *     The public identifier of the plugin session.
 * @param startedAt
 *     The time at which the session started.
 * @param endedAt
 *     The time at which the session ended, if it has ended.
 * @param status
 *     The current session status.
 * @param endReason
 *     The reason the session ended, if available.
 * @param pluginVersion
 *     The AnimatedArchitecture plugin version.
 * @param serverVersion
 *     The Bukkit server version.
 * @param minecraftVersion
 *     The Minecraft version exposed by the server.
 * @param serverSoftware
 *     The server software name.
 */
public record PluginSession(
    UUID uuid,
    Instant startedAt,
    @Nullable Instant endedAt,
    PluginSessionStatus status,
    @Nullable String endReason,
    String pluginVersion,
    String serverVersion,
    String minecraftVersion,
    String serverSoftware
)
{
}
