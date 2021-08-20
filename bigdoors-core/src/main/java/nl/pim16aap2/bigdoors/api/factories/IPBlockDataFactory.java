package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.PBlockData;

import java.util.Optional;

/**
 * Represents a factory interface used to construct {@link PBlockData} objects.
 *
 * @author Pim
 */
public interface IPBlockDataFactory
{
    /**
     * Creates a new {@link PBlockData} of the block at the location if allowed and possible.
     *
     * @param loc    The location at which the {@link ICustomCraftFallingBlock} will be spawned.
     * @param bottom True if this is the lowest block of the object to move.
     * @param radius The radius of the block to an arbitrary point.
     * @return The {@link ICustomCraftFallingBlock} that was constructed.
     */
    Optional<PBlockData> create(IPLocation loc, boolean bottom, float radius, float startAngle)
        throws Exception;

    /**
     * Gets the spawn location of a falling block based on the location of a block.
     *
     * @param loc    The location of the block that will be replaced by an animated block.
     * @param bottom Whether the block is on the bottom row of an animated door.
     * @return The spawn location of the falling block.
     */
    default IPLocation getSpawnLocation(IPLocation loc, boolean bottom)
    {
        // Move the lowest blocks up a little, so the client won't predict they're
        // touching through the ground, which would make them slower than the rest.
        final double offset = bottom ? .010001 : 0;
        return BigDoors.get().getPlatform().getPLocationFactory()
                       .create(loc.getWorld(),
                               loc.getBlockX() + 0.5,
                               loc.getBlockY() - 0.020 + offset,
                               loc.getBlockZ() + 0.5);
    }
}
