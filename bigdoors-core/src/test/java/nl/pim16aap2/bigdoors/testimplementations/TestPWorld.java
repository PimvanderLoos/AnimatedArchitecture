package nl.pim16aap2.bigdoors.testimplementations;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.WorldTime;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class TestPWorld implements IPWorld
{
    private final UUID uuid;
    private final boolean exists;
    private final WorldTime time;

    @Getter(onMethod = @__({@Override}))
    @Setter
    private String name = "testWorld";

    public TestPWorld(final @NotNull UUID uuid)
    {
        this(uuid, new WorldTime(0), true);
    }

    public TestPWorld(final @NotNull UUID uuid, final @NotNull WorldTime time)
    {
        this(uuid, time, true);
    }

    public TestPWorld(final @NotNull UUID uuid, final boolean exists)
    {
        this(uuid, new WorldTime(0), exists);
    }

    public TestPWorld(final @NotNull UUID uuid, final @NotNull WorldTime time, final boolean exists)
    {
        this.uuid = uuid;
        this.exists = exists;
        this.time = time;
    }


    @Override
    public @NotNull UUID getUID()
    {
        return uuid;
    }

    @Override
    public boolean exists()
    {
        return exists;
    }

    @Override
    public @NotNull WorldTime getTime()
    {
        return time;
    }

    @Override
    public @NotNull IPWorld clone()
    {
        return new TestPWorld(uuid, time, exists);
    }
}
