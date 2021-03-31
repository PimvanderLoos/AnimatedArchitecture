package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;
import java.util.function.Function;

public enum Limit
{
    DOOR_SIZE("doorsize", IConfigLoader::maxDoorSize),
    DOOR_COUNT("doorcount", IConfigLoader::maxDoorCount),
    POWERBLOCK_DISTANCE("powerblockdistance", IConfigLoader::maxPowerBlockDistance),
    BLOCKS_TO_MOVE("blockstomove", IConfigLoader::maxBlocksToMove),
    ;

    @Getter
    @NotNull final String userPermission;
    @Getter
    @NotNull final String adminPermission;
    @NotNull final Function<IConfigLoader, OptionalInt> globalLimitSupplier;

    Limit(final @NotNull String permissionName, final @NotNull Function<IConfigLoader, OptionalInt> globalLimitSupplier)
    {
        userPermission = "bigdoors.limit." + permissionName + ".";
        adminPermission = "bigdoors.admin.bypass.limit." + permissionName;
        this.globalLimitSupplier = globalLimitSupplier;
    }

    public @NotNull OptionalInt getGlobalLimit(final @NonNull IConfigLoader configLoader)
    {
        return globalLimitSupplier.apply(configLoader);
    }
}
