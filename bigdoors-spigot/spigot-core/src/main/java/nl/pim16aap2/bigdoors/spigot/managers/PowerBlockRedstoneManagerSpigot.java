package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.spigot.factories.plocationfactory.PLocationFactorySpigot;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IPowerBlockRedstoneManager} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public final class PowerBlockRedstoneManagerSpigot implements IPowerBlockRedstoneManager
{
    private final PLocationFactorySpigot locationFactory;

    @Inject
    public PowerBlockRedstoneManagerSpigot(PLocationFactorySpigot locationFactory)
    {
        this.locationFactory = locationFactory;
    }

    @Override
    public boolean isBlockPowered(IPWorld world, Vector3Di position)
    {
        return locationFactory.create(world, position).getBlock().getBlockPower() > 0;
    }
}
