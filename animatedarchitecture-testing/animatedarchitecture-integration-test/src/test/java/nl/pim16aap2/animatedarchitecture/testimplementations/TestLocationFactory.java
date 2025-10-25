package nl.pim16aap2.animatedarchitecture.testimplementations;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;

public class TestLocationFactory implements ILocationFactory
{
    @Override
    public ILocation create(IWorld world, double x, double y, double z)
    {
        return new nl.pim16aap2.animatedarchitecture.testimplementations.TestLocation(world, x, y, z);
    }

    @Override
    public ILocation create(IWorld world, IVector3D position)
    {
        return create(world, position.xD(), position.yD(), position.zD());
    }

    @Override
    public ILocation create(String worldName, double x, double y, double z)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public ILocation create(String worldName, IVector3D position)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }
}
