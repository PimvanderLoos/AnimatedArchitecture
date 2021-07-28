package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.WorldTime;
import org.jetbrains.annotations.NotNull;

public final class TestPWorld implements IPWorld
{
    private final String worldName;
    private final boolean exists;
    private final WorldTime time;

    public TestPWorld(final @NotNull String name)
    {
        worldName = name;
        exists = true;
        time = new WorldTime(0);
    }

    @Override
    public @NotNull String worldName()
    {
        return worldName;
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
        return new TestPWorld(worldName);
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
        return worldName.equals(((IPWorld) o).worldName());
    }

    @Override
    public int hashCode()
    {
        return worldName.hashCode();
    }
}
