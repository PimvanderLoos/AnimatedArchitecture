package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IRedstoneManager;
import nl.pim16aap2.bigdoors.spigot.factories.plocationfactory.PLocationFactorySpigot;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IRedstoneManager} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public final class RedstoneManagerSpigot implements IRedstoneManager
{
    private static final boolean REDSTONE_DISABLED = false;

    private final PLocationFactorySpigot locationFactory;

    @Inject
    public RedstoneManagerSpigot(PLocationFactorySpigot locationFactory)
    {
        this.locationFactory = locationFactory;
    }

    @Override
    public RedstoneStatus isBlockPowered(IPWorld world, Vector3Di position)
    {
        if (REDSTONE_DISABLED)
            return RedstoneStatus.DISABLED;

        return locationFactory.create(world, position).getBlock().getBlockPower() > 0 ?
               RedstoneStatus.POWERED : RedstoneStatus.UNPOWERED;
    }
}
