package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;

import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IPowerBlockRedstoneManager} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public final class PowerBlockRedstoneManagerSpigot implements IPowerBlockRedstoneManager
{
    private static final PowerBlockRedstoneManagerSpigot INSTANCE = new PowerBlockRedstoneManagerSpigot();

    private PowerBlockRedstoneManagerSpigot()
    {
    }

    /**
     * Gets the instance of this {@link PowerBlockRedstoneManagerSpigot}.
     *
     * @return The instance of this {@link PowerBlockRedstoneManagerSpigot}.
     */
    public static PowerBlockRedstoneManagerSpigot get()
    {
        return INSTANCE;
    }

    @SuppressWarnings("unused")
    private boolean isPoweredRedstone(Location loc)
    {
//        if (loc.getBlock().isBlockIndirectlyPowered())
        return true;
    }

    // Need to figure out if the new method works. If so, the new method is better.
    // However, I cannot test this right now, so I'm leaving the old code here until I can.
    @SuppressWarnings("CommentedOutCode")
    @Override
    public boolean isBlockPowered(IPWorld world, Vector3Di position)
    {
        final Location loc = new Location(SpigotAdapter.getBukkitWorld(world), position.x(), position.y(),
                                          position.z());
        // TODO: Check if this is sufficient.
        return loc.getBlock().isBlockIndirectlyPowered();

//        for (PBlockFace face : PBlockFace.values())
//        {
//            if (face == PBlockFace.NONE)
//                continue;
//            Vector3Di dirVec = PBlockFace.getDirection(face);
//            if (isPoweredRedstone(loc.clone().add(dirVec.getX(), dirVec.getY(), dirVec.getZ())))
//                return true;
//        }
//        return false;
    }
}
