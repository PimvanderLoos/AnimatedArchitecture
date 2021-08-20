package nl.pim16aap2.bigdoors.api;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class PPlayerData implements IPPlayerDataContainer
{
    private final UUID uuid;

    @Getter
    private final String name;

    @Getter
    private final int doorSizeLimit;

    @Getter
    private final int doorCountLimit;

    @Getter
    private final boolean isOp;

    private final boolean hasProtectionBypassPermission;

    public PPlayerData(UUID uuid, String name, int doorSizeLimit, int doorCountLimit, long permissionsFlag)
    {
        this.uuid = uuid;
        this.name = name;
        this.doorSizeLimit = doorSizeLimit;
        this.doorCountLimit = doorCountLimit;
        isOp = PermissionFlag.hasFlag(PermissionFlag.OP, permissionsFlag);
        hasProtectionBypassPermission = PermissionFlag.hasFlag(PermissionFlag.BYPASS, permissionsFlag);
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        return hasProtectionBypassPermission;
    }
}
