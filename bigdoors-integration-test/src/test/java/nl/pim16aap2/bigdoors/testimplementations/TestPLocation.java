package nl.pim16aap2.bigdoors.testimplementations;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;

public final class TestPLocation implements IPLocation
{
    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    @NonNull
    private IPWorld world;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private double x;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private double y;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private double z;


    public TestPLocation(final @NonNull IPWorld world, final double x, final double y, final double z)
    {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public @NonNull Vector2Di getChunk()
    {
        return new Vector2Di(getBlockX() << 4, getBlockZ() << 4);
    }

    @Override
    public int getBlockX()
    {
        return (int) x;
    }

    @Override
    public int getBlockY()
    {
        return (int) y;
    }

    @Override
    public int getBlockZ()
    {
        return (int) z;
    }

    @Override
    public @NonNull IPLocation add(final double x, final double y, final double z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    public @NonNull IPLocation add(final @NonNull Vector3DiConst vector)
    {
        return add(vector.getX(), vector.getY(), vector.getZ());
    }

    @Override
    public @NonNull IPLocation add(final @NonNull Vector3DdConst vector)
    {
        return add(vector.getX(), vector.getY(), vector.getZ());
    }

    @Override
    public @NonNull IPLocation clone()
    {
        try
        {
            TestPLocation cloned = (TestPLocation) super.clone();
            cloned.world = world.clone();
            return cloned;
        }
        catch (CloneNotSupportedException e)
        {
            // TODO: Only log to file! It's already dumped in the console because it's thrown.
            Error er = new Error(e);
            BigDoors.get().getPLogger().logThrowable(er);
            throw er;
        }
    }

    @Override
    public String toString()
    {
        return toIntPositionString();
    }
}
