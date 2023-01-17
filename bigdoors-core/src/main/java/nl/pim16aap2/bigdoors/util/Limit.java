package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IConfigLoader;

import java.util.OptionalInt;
import java.util.function.Function;

public enum Limit
{
    MOVABLE_SIZE("door_size", IConfigLoader::maxMovableSize),
    MOVABLE_COUNT("door_count", IConfigLoader::maxMovableCount),
    POWERBLOCK_DISTANCE("powerblock_distance", IConfigLoader::maxPowerBlockDistance),
    BLOCKS_TO_MOVE("blocks_to_move", IConfigLoader::maxBlocksToMove),
    ;

    @Getter
    private final String userPermission;
    @Getter
    private final String adminPermission;
    private final Function<IConfigLoader, OptionalInt> globalLimitSupplier;

    Limit(String permissionName, Function<IConfigLoader, OptionalInt> globalLimitSupplier)
    {
        userPermission = "bigdoors.limit." + permissionName + ".";
        adminPermission = "bigdoors.admin.bypass.limit." + permissionName;
        this.globalLimitSupplier = globalLimitSupplier;
    }

    public OptionalInt getGlobalLimit(IConfigLoader configLoader)
    {
        return globalLimitSupplier.apply(configLoader);
    }
}
