package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;

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
     * @param loc
     *     The location at which the {@link IAnimatedBlock} will be spawned.
     * @param bottom
     *     True if this is the lowest block of the object to move.
     * @param radius
     *     The radius of the block to an arbitrary point.
     * @return The {@link IAnimatedBlock} that was constructed.
     */
    Optional<PBlockData> create(
        IPLocation loc, boolean bottom, float radius, float startAngle)
        throws Exception;

    /**
     * Gets the spawn location of a falling block based on the location of a block.
     *
     * @param locationFactory
     *     The {@link IPLocationFactory} to use to create new locations.
     * @param loc
     *     The location of the block that will be replaced by an animated block.
     * @param bottom
     *     Whether the block is on the bottom row of an animated door.
     * @return The spawn location of the falling block.
     */
    default IPLocation getSpawnLocation(IPLocationFactory locationFactory, IPLocation loc, boolean bottom)
    {
        // Move the lowest blocks up a little, so the client won't predict they're
        // touching through the ground, which would make them slower than the rest.
        final double offset = bottom ? 0.010_001 : 0;
        return locationFactory.create(loc.getWorld(),
                                      loc.getBlockX() + 0.5,
                                      loc.getBlockY() - 0.020 + offset,
                                      loc.getBlockZ() + 0.5);
    }
}
