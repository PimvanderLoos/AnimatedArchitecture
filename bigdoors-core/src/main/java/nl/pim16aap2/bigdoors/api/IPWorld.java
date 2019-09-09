package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.WorldTime;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a BigDoors world.
 *
 * @author Pim
 */
public interface IPWorld extends Cloneable
{
    /**
     * Gets the UUID of this world.
     *
     * @return The UUID of this world.
     */
    @NotNull
    UUID getUID();

    /**
     * Checks if this is a valid world in the current {@link IBigDoorsPlatform}.
     *
     * @return True if this is a valid world in the current {@link IBigDoorsPlatform}.
     */
    boolean exists();

    /**
     * Gets the time in this world.
     *
     * @return Gets the time in this world.
     */
    @NotNull
    WorldTime getTime();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IPWorld clone();
}
