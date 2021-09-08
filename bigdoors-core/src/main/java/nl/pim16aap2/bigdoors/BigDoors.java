package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;

/**
 * Represents the core class of BigDoors.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
@Singleton
public final class BigDoors extends RestartableHolder
{
    private static final BigDoors INSTANCE = new BigDoors();

    /**
     * The platform to use. e.g. "Spigot".
     */
    private @Nullable IBigDoorsPlatform platform;

    private BigDoors()
    {
    }

    /**
     * Gets the instance of this class.
     *
     * @return The instance of this class.
     */
    public static BigDoors get()
    {
        return INSTANCE;
    }

    /**
     * Sets the platform implementing BigDoor's internal API.
     *
     * @param platform
     *     The platform implementing BigDoor's internal API.
     */
    public void setBigDoorsPlatform(IBigDoorsPlatform platform)
    {
        this.platform = platform;
    }

    /**
     * gets the platform implementing BigDoor's internal API.
     *
     * @return The platform implementing BigDoor's internal API.
     */
    public IBigDoorsPlatform getPlatform()
    {
        if (platform == null)
            throw new IllegalStateException("No platform currently registered!");
        return platform;
    }

    /**
     * Handles a restart.
     */
    public void restart()
    {
        restartables.forEach(IRestartable::restart);
    }

    /**
     * Handles a shutdown.
     */
    public void shutdown()
    {
        restartables.forEach(IRestartable::shutdown);
    }
}
