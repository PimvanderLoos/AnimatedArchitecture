package nl.pim16aap2.bigdoors.testimplementations;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.WorldTime;

public final class TestPWorld implements IPWorld
{
    @Getter(onMethod = @__({@Override}))
    private final String worldName;
    private final boolean exists;
    private final WorldTime time;

    public TestPWorld(final @NonNull String name)
    {
        worldName = name;
        exists = true;
        time = new WorldTime(0);
    }

    @Override
    public boolean exists()
    {
        return exists;
    }

    @Override
    public @NonNull WorldTime getTime()
    {
        return time;
    }

    @Override
    public @NonNull IPWorld clone()
    {
        return new TestPWorld(worldName);
    }
}
