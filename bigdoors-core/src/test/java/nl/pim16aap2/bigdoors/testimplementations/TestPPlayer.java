package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class TestPPlayer implements IPPlayer
{
    @NotNull
    private final String name;
    @NotNull
    private final UUID uuid;

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
}
