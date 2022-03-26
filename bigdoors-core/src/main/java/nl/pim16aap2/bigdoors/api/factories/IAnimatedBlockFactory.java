package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.bigdoors.api.animatedblockhook.AnimationContext;

import java.util.Optional;

/**
 * Represents a IFactory for {@link IAnimatedBlock} and {@link IAnimatedBlockData}.
 *
 * @author Pim
 */
public interface IAnimatedBlockFactory
{
    /**
     * Creates a new {@link IAnimatedBlock} at the given location made of the provided block.
     *
     * @param loc
     *     The location at which the {@link IAnimatedBlock} will be spawned.
     * @param bottom
     *     True if this is the lowest block of the object to move.
     * @param startAngle
     *     The starting angle of the block to the rotation point.
     * @param radius
     *     The radius of the block to the rotation point.
     * @param context
     *     The animation context of the animated block.
     * @return The {@link IAnimatedBlock} that was constructed if it could be constructed.
     */
    Optional<IAnimatedBlock> create(
        IPLocation loc, float radius, float startAngle, boolean bottom, AnimationContext context)
        throws Exception;
}
