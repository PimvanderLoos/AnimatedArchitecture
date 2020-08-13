package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;
import java.util.function.Supplier;

public enum Limit
{
    DOOR_SIZE("doorsize", BigDoors.get().getPlatform().getConfigLoader()::maxDoorSize),
    DOORS_OWNED("doorsowned", BigDoors.get().getPlatform().getConfigLoader()::maxdoorCount),
    POWERBLOCK_DISTANCE("powerblockdistance", BigDoors.get().getPlatform().getConfigLoader()::maxPowerBlockDistance),
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

    public OptionalInt getGlobalLimit()
    {
        return globalLimitSupplier.get();
    }
}
