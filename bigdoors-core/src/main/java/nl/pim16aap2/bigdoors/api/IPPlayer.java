package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a BigDoors player.
 *
 * @author Pim
 */
public interface IPPlayer
{
    /**
     * Gets the name of this player.
     *
     * @return The name of this player.
     */
    @NotNull
    String getName();

    /**
     * Gets the UUID of this player.
     *
     * @return The UUID of this player.
     */
    @NotNull
    UUID getUUID();
}
