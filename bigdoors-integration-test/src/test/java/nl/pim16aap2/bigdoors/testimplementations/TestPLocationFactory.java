package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

public class TestPLocationFactory implements IPLocationFactory
{
    @Override
    public IPLocation create(final IPWorld world, final double x, final double y, final double z)
    {
        return new TestPLocation(world, x, y, z);
    }

    @Override
    public IPLocation create(final IPWorld world, final Vector3Di position)
    {
        return create(world, position.x(), position.y(), position.z());
    }

    @Override
    public IPLocation create(final IPWorld world, final Vector3Dd position)
    {
        return create(world, position.x(), position.y(), position.z());
    }

    @Override
    public IPLocation create(final String worldName, final double x, final double y, final double z)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldName), x, y, z);
    }

    @Override
    public IPLocation create(final String worldName, final Vector3Di position)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldName),
                      position.x(), position.y(), position.z());
    }

    @Override
    public IPLocation create(final String worldName, final Vector3Dd position)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldName),
                      position.x(), position.y(), position.z());
    }
}
