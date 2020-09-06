package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

public class TestPermissionsManager implements IPermissionsManager
{
    public OptionalInt maxPermissionSuffix = OptionalInt.empty();
    public boolean hasPermission = true;
    public boolean isOp;

    @Override
    public OptionalInt getMaxPermissionSuffix(final @NotNull IPPlayer player, final @NotNull String permissionBase)
    {
        return maxPermissionSuffix;
    }

    @Override
    public boolean hasPermission(final @NotNull IPPlayer player, final @NotNull String permissionNode)
    {
        return hasPermission;
    }

    @Override
    public boolean isOp(@NotNull IPPlayer player)
    {
        return isOp;
    }
}
