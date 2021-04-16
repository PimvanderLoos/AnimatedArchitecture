package nl.pim16aap2.bigdoors.testimplementations;

import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class TestPPlayer implements IPPlayer
{
    private @Nullable String lastMessage, beforeLastMessage;

    private @NonNull final PPlayerData playerData;

    @Setter
    private boolean hasProtectionBypassPermission = false;

    public TestPPlayer(final @NonNull PPlayerData playerData)
    {
        this.playerData = playerData;
    }

    @Override
    public @NonNull String getName()
    {
        return playerData.getName();
    }

    @Override
    public @NonNull UUID getUUID()
    {
        return playerData.getUUID();
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        return false;
    }

    @Override
    public @NonNull Optional<IPLocation> getLocation()
    {
        return Optional.empty();
    }

    @Override
    public int getDoorSizeLimit()
    {
        return 0;
    }

    @Override
    public int getDoorCountLimit()
    {
        return 0;
    }

    @Override
    public boolean isOp()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return asString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        return getUUID().equals(((TestPPlayer) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }

    @Override
    public void sendMessage(final @NonNull Level level, final @NonNull String message)
    {
        beforeLastMessage = lastMessage;
        lastMessage = message;
        BigDoors.get().getPLogger().logMessage(level, message);
    }

    public @Nullable String getLastMessage()
    {
        return lastMessage;
    }

    public @Nullable String getBeforeLastMessage()
    {
        return beforeLastMessage;
    }

    @Override
    public @NonNull PPlayerData getPPlayerData()
    {
        return playerData;
    }
}
