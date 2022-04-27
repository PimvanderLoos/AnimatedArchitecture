package nl.pim16aap2.bigdoors.spigot.factories.plocationfactory;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IPLocationFactory} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public class PLocationFactorySpigot implements IPLocationFactory
{
    private final IPWorldFactory worldFactory;

    @Inject
    public PLocationFactorySpigot(IPWorldFactory worldFactory)
    {
        this.worldFactory = worldFactory;
    }

    @Override
    public IPLocation create(IPWorld world, double x, double y, double z)
    {
        return new PLocationSpigot(world, x, y, z);
    }

    @Override
    public IPLocation create(IPWorld world, IVector3D position)
    {
        return create(world, position.xD(), position.yD(), position.zD());
    }

    @Override
    public IPLocation create(String worldName, double x, double y, double z)
    {
        return create(worldFactory.create(worldName), x, y, z);
    }

    @Override
    public IPLocation create(String worldName, IVector3D position)
    {
        return create(worldName, position.xD(), position.yD(), position.zD());
    }
}
