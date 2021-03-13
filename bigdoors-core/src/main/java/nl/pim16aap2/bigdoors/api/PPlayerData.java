package nl.pim16aap2.bigdoors.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

@AllArgsConstructor
public class PPlayerData implements IPPlayerDataContainer
{
    @Getter
    private @NonNull String name;

    private @NonNull UUID uuid;

    @Getter
    private int doorSizeLimit;

    @Getter
    private int doorCountLimit;

    @Getter
    private boolean isOp;

    private boolean hasProtectionBypassPermission;

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
