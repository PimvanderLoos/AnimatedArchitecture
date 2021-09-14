package nl.pim16aap2.bigdoors.spigot.util.api;

public interface ISubPlatformManagerSpigot
{
    /**
     * Gets the version of the registered sub-platform.
     *
     * @return The version of the registered sub-platform.
     */
    String getSubPlatformVersion();

    /**
     * Gets the server version.
     *
     * @return The server version.
     */
    String getServerVersion();

    /**
     * Checks if a sub-platform was registered successfully.
     *
     * @return True if a sub-platform was registered successfully.
     */
    boolean isValidPlatform();

    /**
     * Gets the currently-registered sub-platform.
     *
     * @return The sub-platform that was registered.
     *
     * @throws IllegalStateException
     *     When no platform is registered. See {@link #isValidPlatform()}.
     */
    IBigDoorsSpigotSubPlatform getSpigotPlatform();
}
