package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.logging.Level;

/**
 * Represents the configured settings.
 *
 * @author Pim
 */
public interface IConfig extends IRestartable
{
    /**
     * @return True if debug mode is enabled.
     */
    boolean debug();

    /**
     * @return True if redstone is enabled.
     */
    boolean isRedstoneEnabled();

    /**
     * The amount of time a user gets to specify which structure they meant in case of structureID collisions.
     * <p>
     * This can happen in case they specified a structure by its name when they own more than 1 structure with that
     * name.
     *
     * @return The amount of time (in seconds) to give a user to specify which structure they meant.
     */
    default int specificationTimeout()
    {
        return 20;
    }

    /**
     * @return The movement formula of a flag.
     */
    String flagMovementFormula();

    /**
     * Gets the number of ticks a structure should wait before it can be activated again.
     *
     * @return The number of ticks a structure should wait before it can be activated again.
     */
    int coolDown();

    Locale locale();

    /**
     * Gets the global maximum number of blocks that can be in a structure.
     * <p>
     * Structures exceeding this limit cannot be created or activated.
     *
     * @return The global maximum number of blocks that can be in a structure.
     */
    OptionalInt maxStructureSize();

    /**
     * Gets the amount of time (in minutes) power blocks should be kept in cache.
     *
     * @return The amount of time power blocks should be kept in cache.
     */
    int cacheTimeout();

    /**
     * Gets the global maximum number of structures a player can own.
     *
     * @return The global maximum number of structures a player can own.
     */
    OptionalInt maxStructureCount();

    /**
     * Gets the global maximum distance (in blocks) a powerblock can be from the structure.
     *
     * @return The global maximum distance (in blocks) a powerblock can be from the structure.
     */
    OptionalInt maxPowerBlockDistance();

    /**
     * Gets the global maximum number of blocks a structure can move for applicable types (e.g. sliding door).
     *
     * @return The global maximum number of blocks a structure can move for applicable types (e.g. sliding door).
     */
    OptionalInt maxBlocksToMove();

    /**
     * @return True if we should try to load any unloaded chunks for a toggle.
     */
    boolean loadChunksForToggle();

    /**
     * Gets the structure price formula for a specific type of structure.
     *
     * @param type
     *     The structure type.
     * @return The formula for the structure type.
     */
    String getPrice(StructureType type);

    /**
     * Gets the animation time multiplier for a specific type of structure.
     *
     * @param type
     *     The structure type.
     * @return The animation time multiplier for the structure type.
     */
    double getAnimationTimeMultiplier(StructureType type);

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
