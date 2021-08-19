package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IConfigLoader;

import java.util.OptionalInt;
import java.util.function.Function;

public enum Limit
{
    DOOR_SIZE("doorsize", IConfigLoader::maxDoorSize),
    DOOR_COUNT("doorcount", IConfigLoader::maxDoorCount),
    POWERBLOCK_DISTANCE("powerblockdistance", IConfigLoader::maxPowerBlockDistance),
    BLOCKS_TO_MOVE("blockstomove", IConfigLoader::maxBlocksToMove),
    ;

    @Getter final String userPermission;
    @Getter final String adminPermission;
    final Function<IConfigLoader, OptionalInt> globalLimitSupplier;

    Limit(final String permissionName, final Function<IConfigLoader, OptionalInt> globalLimitSupplier)
    {
        userPermission = "bigdoors.limit." + permissionName + ".";
        adminPermission = "bigdoors.admin.bypass.limit." + permissionName;
        this.globalLimitSupplier = globalLimitSupplier;
    }

    public OptionalInt getGlobalLimit(final IConfigLoader configLoader)
    {
        return globalLimitSupplier.apply(configLoader);
    }
}
