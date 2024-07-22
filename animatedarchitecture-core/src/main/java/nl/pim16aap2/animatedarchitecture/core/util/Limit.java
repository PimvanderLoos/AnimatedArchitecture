package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;

import java.util.OptionalInt;
import java.util.function.Function;

/**
 * Represents a limit that can be set for a player.
 * <p>
 * Each limit has a user permission and an admin permission. The user permission is used to determine the limit for a
 * player by checking the highest numerical suffix of the permission node. The admin permission is used to bypass the
 * limit altogether.
 */
public enum Limit
{
    /**
     * The maximum size a structure can have in number of blocks.
     */
    STRUCTURE_SIZE("structure_size", IConfig::maxStructureSize),

    /**
     * The maximum number of structures a player can own.
     */
    STRUCTURE_COUNT("structure_count", IConfig::maxStructureCount),

    /**
     * The maximum distance a power block can be from a structure.
     */
    POWERBLOCK_DISTANCE("powerblock_distance", IConfig::maxPowerBlockDistance),

    /**
     * The maximum distance a structure can move measured in blocks.
     * <p>
     * This does not take the number of blocks the structure actually moves into account, only the distance the
     * structure moves.
     */
    BLOCKS_TO_MOVE("blocks_to_move", IConfig::maxBlocksToMove),
    ;

    /**
     * The permission node for the user to determine the limit.
     * <p>
     * The limit for a player is determined by checking the highest numerical suffix of this permission node.
     *
     * @return The permission node for the user to determine the limit.
     */
    @Getter
    private final String userPermission;

    /**
     * The permission node for an admin to bypass the limit.
     *
     * @return The permission node for an admin to bypass the limit.
     */
    @Getter
    private final String adminPermission;

    /**
     * The supplier for the global limit.
     * <p>
     * This should be a reference to the method in the {@link IConfig} implementation that returns the global limit for
     * this limit.
     */
    private final Function<IConfig, OptionalInt> globalLimitSupplier;

    Limit(String permissionName, Function<IConfig, OptionalInt> globalLimitSupplier)
    {
        userPermission = Constants.PERMISSION_PREFIX_USER + "limit." + permissionName;
        adminPermission = Constants.PERMISSION_PREFIX_ADMIN_BYPASS_LIMIT + permissionName;
        this.globalLimitSupplier = globalLimitSupplier;
    }

    /**
     * Gets the global limit for this limit.
     *
     * @param config
     *     The config to get the global limit from.
     * @return The global limit for this limit as defined in the config.
     */
    public OptionalInt getGlobalLimit(IConfig config)
    {
        return globalLimitSupplier.apply(config);
    }
}
