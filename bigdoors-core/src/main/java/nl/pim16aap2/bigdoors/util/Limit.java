package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;
import java.util.function.Supplier;

public enum Limit
{
    DOOR_SIZE("doorsize", BigDoors.get().getPlatform().getConfigLoader()::maxDoorSize),
    DOOR_COUNT("doorcount", BigDoors.get().getPlatform().getConfigLoader()::maxDoorCount),
    POWERBLOCK_DISTANCE("powerblockdistance", BigDoors.get().getPlatform().getConfigLoader()::maxPowerBlockDistance),
    BLOCKS_TO_MOVE("blockstomove", BigDoors.get().getPlatform().getConfigLoader()::maxBlocksToMove),
    ;

    @Getter
    @NotNull
    final String userPermission;
    @Getter
    @NotNull
    final String adminPermission;
    @NotNull
    final Supplier<OptionalInt> globalLimitSupplier;

    Limit(final @NotNull String permissionName, final @NotNull Supplier<OptionalInt> globalLimitSupplier)
    {
        userPermission = "bigdoors.limit." + permissionName + ".";
        adminPermission = "bigdoors.admin.bypass.limit." + permissionName;
        this.globalLimitSupplier = globalLimitSupplier;
    }

    public @NotNull OptionalInt getGlobalLimit()
    {
        return globalLimitSupplier.get();
    }
}
