package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.logging.Level;

// TODO: Change this description and while I'm at it, also the name of the class.

/**
 * Represents a config with general options.
 *
 * @author Pim
 */
public interface IConfigLoader extends IRestartable
{
    /**
     * Checks if debug mode is enabled.
     *
     * @return True if debug mode is enabled.
     */
    boolean debug();

    /**
     * The amount of time a user gets to specify which door they meant in case of doorID collisions.
     * <p>
     * This can happen in case they specified a door by its name when they own more than 1 door with that name.
     *
     * @return The amount of time (in seconds) to give a user to specify which door they meant.
     */
    default int specificationTimeout()
    {
        return 20;
    }

    /**
     * Gets the formula of a flag. Used for debugging.
     *
     * @return The formula of a flag.
     */
    String flagFormula();

    /**
     * Gets the number of ticks a door should wait before it can be activated again.
     *
     * @return The number of ticks a door should wait before it can be activated again.
     */
    int coolDown();

    /**
     * Checks if stats gathering is allowed.
     *
     * @return True if stats gathering is allowed.
     */
    boolean allowStats();

    Locale locale();

    /**
     * Gets the global maximum number of blocks that can be in a door.
     * <p>
     * Doors exceeding this limit cannot be created or activated.
     *
     * @return The global maximum number of blocks that can be in a door.
     */
    OptionalInt maxDoorSize();

    /**
     * Gets the amount of time (in minutes) power blocks should be kept in cache.
     *
     * @return The amount of time power blocks should be kept in cache.
     */
    int cacheTimeout();

    /**
     * Gets the global maximum number of doors a player can own.
     *
     * @return The global maximum number of doors a player can own.
     */
    OptionalInt maxDoorCount();

    /**
     * Gets the global maximum distance (in blocks) a powerblock can be from the door.
     *
     * @return The global maximum distance (in blocks) a powerblock can be from the door.
     */
    OptionalInt maxPowerBlockDistance();

    /**
     * Gets the global maximum number of blocks a door can move for applicable types (e.g. sliding door).
     *
     * @return The global maximum number of blocks a door can move for applicable types (e.g. sliding door).
     */
    OptionalInt maxBlocksToMove();

    /**
     * Checks if updates should be downloaded automatically.
     *
     * @return True is updates should be downloaded automatically.
     */
    boolean autoDLUpdate();

    /**
     * Gets the amount time (in seconds) to wait before downloading an update. If set to 24 hours (86400 seconds), and
     * an update was released on Monday June 1 at 12PM, it will not download this update before Tuesday June 2 at 12PM.
     * When running a dev-build, however, this value is overridden to 0.
     *
     * @return The amount time (in seconds) to wait before downloading an update.
     */
    long downloadDelay();

    /**
     * Checks if redstone should be used to toggle doors.
     *
     * @return True if redstone should be used to toggle doors.
     */
    boolean enableRedstone();

    /**
     * @return True if we should try to load any unloaded chunks for a toggle.
     */
    boolean loadChunksForToggle();

    /**
     * Whether to check for updates.
     *
     * @return True if the plugin should check for new updates.
     */
    boolean checkForUpdates();

    /**
     * Gets the door price formula for a specific type of door.
     *
     * @param type
     *     The door type.
     * @return The formula for the door type.
     */
    String getPrice(DoorType type);

    /**
     * Gets the animation time multiplier for a specific type of door.
     *
     * @param type
     *     The door type.
     * @return The animation time multiplier for the door type.
     */
    double getAnimationSpeedMultiplier(DoorType type);

    /**
     * @return The global maximum speed of a block.
     */
    double maxBlockSpeed();

    /**
     * The log level to use.
     *
     * @return The log level.
     */
    Level logLevel();

    /**
     * Checks if errors should be logged to the console.
     *
     * @return True if errors should be logged to the console.
     */
    boolean consoleLogging();
}
