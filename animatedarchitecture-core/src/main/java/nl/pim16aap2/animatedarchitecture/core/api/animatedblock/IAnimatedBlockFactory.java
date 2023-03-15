package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

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
     * @param radius
     *     The radius of the block to the rotation point.
     * @param startAngle
     *     The starting angle of the block to the rotation point.
     * @param bottom
     *     True if this is the lowest block of the object to move.
     * @param onEdge
     *     True if this animated block is on the edge of the cuboid being animated.
     * @param finalPosition
     *     The final position of the block. This is the place where the block will be placed after the animation
     *     finishes.
     * @param context
     *     The animation context of the animated block.
     * @param movementMethod
     *     The movement method of the animated block.
     * @return The {@link IAnimatedBlock} that was constructed if it could be constructed.
     */
    Optional<IAnimatedBlock> create(
        ILocation loc,
        float radius,
        float startAngle,
        boolean bottom,
        boolean onEdge,
        Vector3Dd rotationPoint,
        AnimationContext context,
        Vector3Dd finalPosition,
        Animator.MovementMethod movementMethod)
        throws Exception;
}
