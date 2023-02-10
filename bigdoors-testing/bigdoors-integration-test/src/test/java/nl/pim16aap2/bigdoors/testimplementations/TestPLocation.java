package nl.pim16aap2.bigdoors.testimplementations;

import lombok.Getter;
import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.bigdoors.core.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

public final class TestPLocation implements IPLocation
{
    @Getter
    private final IPWorld world;

    @Getter
    private double x;

    @Getter
    private double y;

    @Getter
    private double z;


    public TestPLocation(IPWorld world, double x, double y, double z)
    {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Vector2Di getChunk()
    {
        return new Vector2Di(getBlockX() << 4, getBlockZ() << 4);
    }

    @Override
    public int getBlockX()
    {
        return MathUtil.floor(x);
    }

    @Override
    public int getBlockY()
    {
        return MathUtil.floor(y);
    }

    @Override
    public int getBlockZ()
    {
        return MathUtil.floor(z);
    }

    @Override
    public IPLocation setX(double newVal)
    {
        return new TestPLocation(world, x + newVal, y, z);
    }

    @Override
    public IPLocation setY(double newVal)
    {
        return new TestPLocation(world, x, y + newVal, z);
    }

    @Override
    public IPLocation setZ(double newVal)
    {
        return new TestPLocation(world, x, y, z + newVal);
    }

    @Override
    public IPLocation add(double x, double y, double z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    public IPLocation add(Vector3Di vector)
    {
        return add(vector.x(), vector.y(), vector.z());
    }

    @Override
    public IPLocation add(Vector3Dd vector)
    {
        return add(vector.x(), vector.y(), vector.z());
    }

    @Override
    public String toString()
    {
        return toIntPositionString();
    }
}
