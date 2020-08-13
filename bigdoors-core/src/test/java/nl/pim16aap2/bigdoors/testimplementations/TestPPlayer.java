package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Level;

public final class TestPPlayer implements IPPlayer
{
    @NotNull
    private final String name;
    @NotNull
    private final UUID uuid;
    @Nullable
    private String lastMessage, beforeLastMessage;

    public TestPPlayer(final @NotNull UUID uuid, final @NotNull String name)
    {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    @NotNull
    public String getName()
    {
        return name;
    }

    @Override
    @NotNull
    public UUID getUUID()
    {
        return uuid;
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
    public void sendMessage(final @NotNull Level level, final @NotNull String message)
    {
        beforeLastMessage = lastMessage;
        lastMessage = message;
        TestMessageableServer.get().sendMessage(level, message);
    }

    @Nullable
    public String getLastMessage()
    {
        return lastMessage;
    }

    @Nullable
    public String getBeforeLastMessage()
    {
        return beforeLastMessage;
    }
}
