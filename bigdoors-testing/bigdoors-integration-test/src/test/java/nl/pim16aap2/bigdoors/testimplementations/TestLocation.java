package nl.pim16aap2.bigdoors.testimplementations;

import lombok.Getter;
import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.bigdoors.core.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

public final class TestLocation implements ILocation
{
    @Getter
    private final IWorld world;

    @Getter
    private double x;

    @Getter
    private double y;

    @Getter
    private double z;


    public TestLocation(IWorld world, double x, double y, double z)
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
    public ILocation setX(double newVal)
    {
        return new TestLocation(world, x + newVal, y, z);
    }

    @Override
    public ILocation setY(double newVal)
    {
        return new TestLocation(world, x, y + newVal, z);
    }

    @Override
    public ILocation setZ(double newVal)
    {
        return new TestLocation(world, x, y, z + newVal);
    }

    @Override
    public ILocation add(double x, double y, double z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    public ILocation add(Vector3Di vector)
    {
        return add(vector.x(), vector.y(), vector.z());
    }

    @Override
    public ILocation add(Vector3Dd vector)
    {
        return add(vector.x(), vector.y(), vector.z());
    }

    @Override
    public String toString()
    {
        return toIntPositionString();
    }
}
