package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a factory for {@link IPPlayer} objects.
 *
 * @author Pim
 */
public interface IPPlayerFactory
{
    /**
     * Creates a new IPWorld.
     *
     * @param playerUUID The UUID of the player.
     * @param playerName The name of the player.
     * @return A new IPWorld object.
     */
    @NotNull IPPlayer create(final @NotNull UUID playerUUID, final @NotNull String playerName);
}
