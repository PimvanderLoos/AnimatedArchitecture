package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the core class of BigDoors.
 *
 * @author Pim
 */
public final class BigDoors
{
    private static final BigDoors instance = new BigDoors();

    /**
     * The platform to use. e.g. "Spigot".
     */
    private IBigDoorsPlatform platform;

    /**
     * Gets the instance of this class.
     *
     * @return The instance of this class.
     */
    public static BigDoors get()
    {
        return instance;
    }

    /**
     * Sets the platform implementing BigDoor's internal API.
     *
     * @param platform The platform implementing BigDoor's internal API.
     */
    public void setBigDoorsPlatform(final @NotNull IBigDoorsPlatform platform)
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
        return platform;
    }
}
