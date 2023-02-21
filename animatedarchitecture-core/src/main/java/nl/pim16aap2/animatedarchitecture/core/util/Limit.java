package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;

import java.util.OptionalInt;
import java.util.function.Function;

public enum Limit
{
    STRUCTURE_SIZE("door_size", IConfig::maxStructureSize),
    STRUCTURE_COUNT("door_count", IConfig::maxStructureCount),
    POWERBLOCK_DISTANCE("powerblock_distance", IConfig::maxPowerBlockDistance),
    BLOCKS_TO_MOVE("blocks_to_move", IConfig::maxBlocksToMove),
    ;

    @Getter
    private final String userPermission;
    @Getter
    private final String adminPermission;
    private final Function<IConfig, OptionalInt> globalLimitSupplier;

    Limit(String permissionName, Function<IConfig, OptionalInt> globalLimitSupplier)
    {
        userPermission = "animatedarchitecture.limit." + permissionName + ".";
        adminPermission = "animatedarchitecture.admin.bypass.limit." + permissionName;
        this.globalLimitSupplier = globalLimitSupplier;
    }

    public OptionalInt getGlobalLimit(IConfig config)
    {
        return globalLimitSupplier.apply(config);
    }
}
