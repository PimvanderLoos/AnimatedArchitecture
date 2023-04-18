package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;

import java.util.OptionalInt;
import java.util.function.Function;

public enum Limit
{
    STRUCTURE_SIZE("doorsize", IConfig::maxStructureSize),
    STRUCTURE_COUNT("doorcount", IConfig::maxStructureCount),
    POWERBLOCK_DISTANCE("powerblockdistance", IConfig::maxPowerBlockDistance),
    BLOCKS_TO_MOVE("blockstomove", IConfig::maxBlocksToMove),
    ;

    @Getter
    private final String userPermission;
    @Getter
    private final String adminPermission;
    private final Function<IConfig, OptionalInt> globalLimitSupplier;

    Limit(String permissionName, Function<IConfig, OptionalInt> globalLimitSupplier)
    {
        userPermission = Constants.PERMISSION_PREFIX_USER + "limit." + permissionName;
        adminPermission = Constants.PERMISSION_PREFIX_ADMIN_BYPASS_LIMIT + permissionName;
        this.globalLimitSupplier = globalLimitSupplier;
    }

    public OptionalInt getGlobalLimit(IConfig config)
    {
        return globalLimitSupplier.apply(config);
    }
}
