package nl.pim16aap2.bigdoors.managers;

import lombok.experimental.UtilityClass;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.Limit;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

@UtilityClass
public class LimitsManager
{
    public OptionalInt getLimit(final @NotNull IPPlayer player, final @NotNull Limit limit)
    {
        final boolean hasBypass = BigDoors.get().getPlatform().getPermissionsManager()
                                          .hasPermission(player, limit.getAdminPermission());
        final @NotNull OptionalInt globalLimit = limit.getGlobalLimit();
        if (hasBypass)
            return globalLimit;

        final @NotNull OptionalInt playerLimit = BigDoors.get().getPlatform().getPermissionsManager()
                                                         .getMaxPermissionSuffix(player, limit.getUserPermission());

        if (globalLimit.isPresent() && playerLimit.isPresent())
            return OptionalInt.of(Math.min(globalLimit.getAsInt(), playerLimit.getAsInt()));

        return globalLimit.isPresent() ? OptionalInt.of(globalLimit.getAsInt()) :
               playerLimit.isPresent() ? OptionalInt.of(playerLimit.getAsInt()) :
               OptionalInt.empty();
    }
}
