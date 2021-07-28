package nl.pim16aap2.bigdoors.api;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class PPlayerData implements IPPlayerDataContainer
{
    private final @NotNull UUID uuid;

    @Getter
    private final @NotNull String name;

    @Getter
    private final int doorSizeLimit;

    @Getter
    private final int doorCountLimit;

    @Getter
    private final boolean isOp;

    private final boolean hasProtectionBypassPermission;

    public PPlayerData(final @NotNull UUID uuid, final @NotNull String name, final int doorSizeLimit,
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
    public @NotNull UUID getUUID()
    {
        return uuid;
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        return hasProtectionBypassPermission;
    }
}
