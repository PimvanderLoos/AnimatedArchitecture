package nl.pim16aap2.animatedarchitecture.core.config;

import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.logging.Level;

/**
 * Represents the configured settings.
 */
public interface IConfig extends IRestartable
{
    /**
     * Returns whether debug mode is enabled.
     *
     * @return True if debug mode is enabled.
     */
    boolean debug();

    /**
     * Returns whether redstone is enabled.
     *
     * @return True if redstone is enabled.
     */
    boolean allowRedstone();

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
     * Returns the movement formula of a flag.
     *
     * @return The movement formula of a flag.
     */
    String flagMovementFormula();

    /**
     * The default locale to use.
     *
     * @return The default locale to use.
     */
    Locale locale();

    /**
     * Whether to allow the client locale to be used.
     *
     * @return True to try to use a player's locale.
     */
    boolean allowClientLocale();

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
    int powerblockCacheTimeout();

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
    OptionalInt maxPowerblockDistance();

    /**
     * Gets the global maximum number of blocks a structure can move for applicable types (e.g. sliding door).
     *
     * @return The global maximum number of blocks a structure can move for applicable types (e.g. sliding door).
     */
    OptionalInt maxBlocksToMove();

    /**
     * Returns the global maximum speed of a block in blocks per second.
     *
     * @return The global maximum speed of a block.
     */
    OptionalDouble maxBlockSpeed();

    /**
     * Returns whether we should try to load any unloaded chunks for a toggle.
     *
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
    String priceFormula(StructureType type);

    /**
     * Gets the animation time multiplier for a specific type of structure.
     *
     * @param type
     *     The structure type.
     * @return The animation time multiplier for the structure type.
     */
    double animationTimeMultiplier(StructureType type);

    /**
     * Whether to skip all animations by default. If true, toggling a structure will simply teleport the blocks to their
     * destination without any animations. For structures that don't have a destination, any toggle request will be
     * ignored.
     *
     * @return True if all animations should be skipped by default.
     */
    boolean skipAnimationsByDefault();

    /**
     * The log level to use.
     *
     * @return The log level.
     */
    Level logLevel();

    /**
     * Reloads the configuration file.
     */
    void reloadConfig();

    /**
     * Gets the primary command name for the plugin.
     * <p>
     * This is the first alias in the command aliases list, or a default value if no aliases are configured.
     *
     * @return The primary command name.
     */
    String primaryCommandName();
}
