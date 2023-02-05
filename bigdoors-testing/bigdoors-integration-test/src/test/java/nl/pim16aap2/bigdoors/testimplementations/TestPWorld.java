package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.util.WorldTime;
import org.jetbrains.annotations.Nullable;

public final class TestPWorld implements IPWorld
{
    private final String worldName;
    private final boolean exists;
    private final WorldTime time;

    public TestPWorld(String name)
    {
        worldName = name;
        exists = true;
        time = new WorldTime(0);
    }

    @Override
    public String worldName()
    {
        return worldName;
    }

    @Override
    public boolean exists()
    {
        return exists;
    }

    @Override
    public WorldTime getTime()
    {
        return time;
    }

    @Override
    public boolean equals(@Nullable Object o)
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
