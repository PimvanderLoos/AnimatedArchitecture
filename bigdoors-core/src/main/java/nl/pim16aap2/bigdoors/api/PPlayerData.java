package nl.pim16aap2.bigdoors.api;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

@AllArgsConstructor
@EqualsAndHashCode
public final class PPlayerData implements IPPlayerDataContainer
{
    private final @NonNull UUID uuid;

    @Getter
    private final @NonNull String name;

    @Getter
    private final int doorSizeLimit;

    @Getter
    private final int doorCountLimit;

    @Getter
    private final boolean isOp;

    private final boolean hasProtectionBypassPermission;

    public PPlayerData(final @NonNull UUID uuid, final @NonNull String name, final int doorSizeLimit,
                       final int doorCountLimit, final long permissionsFlag)
    {
        this.uuid = uuid;
        this.name = name;
        this.doorSizeLimit = doorSizeLimit;
        this.doorCountLimit = doorCountLimit;
        isOp = PermissionFlag.hasFlag(PermissionFlag.OP, permissionsFlag);
        hasProtectionBypassPermission = PermissionFlag.hasFlag(PermissionFlag.BYPASS, permissionsFlag);
    }

    @Override
    public @NonNull UUID getUUID()
    {
        return uuid;
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        return hasProtectionBypassPermission;
    }
}
