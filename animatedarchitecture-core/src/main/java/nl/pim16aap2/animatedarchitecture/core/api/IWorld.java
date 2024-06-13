package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.WorldTime;

/**
 * Represents a AnimatedArchitecture world.
 */
public interface IWorld
{
    /**
     * Gets the name of this world.
     *
     * @return The name of this world.
     */
    String worldName();

    /**
     * Checks if this is a valid world in the current {@link IAnimatedArchitecturePlatform}.
     *
     * @return True if this is a valid world in the current {@link IAnimatedArchitecturePlatform}.
     */
    boolean exists();

    /**
     * Gets the time in this world.
     *
     * @return Gets the time in this world.
     */
    WorldTime getTime();
}
