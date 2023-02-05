package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;

public class TestPLocationFactory implements IPLocationFactory
{
    @Override
    public IPLocation create(IPWorld world, double x, double y, double z)
    {
        return new TestPLocation(world, x, y, z);
    }

    @Override
    public IPLocation create(IPWorld world, IVector3D position)
    {
        return create(world, position.xD(), position.yD(), position.zD());
    }

    @Override
    public IPLocation create(String worldName, double x, double y, double z)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public IPLocation create(String worldName, IVector3D position)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }
}
