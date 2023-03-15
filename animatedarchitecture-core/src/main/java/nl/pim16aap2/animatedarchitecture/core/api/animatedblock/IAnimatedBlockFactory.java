package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

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
     * @param world
     * @param startPosition
     * @param radius
     *     The radius of the block to the rotation point.
     * @param bottom
     *     True if this is the lowest block of the object to move.
     * @param onEdge
     *     True if this animated block is on the edge of the cuboid being animated.
     * @param context
     *     The animation context of the animated block.
     * @param finalPosition
     *     The final position of the block. This is the place where the block will be placed after the animation
     *     finishes.
     * @param movementMethod
     *     The movement method of the animated block.
     * @param blockDataRotator
     * @return The {@link IAnimatedBlock} that was constructed if it could be constructed.
     */
    Optional<IAnimatedBlock> create(
        IWorld world,
        RotatedPosition startPosition,
        float radius,
        boolean bottom,
        boolean onEdge,
        AnimationContext context,
        RotatedPosition finalPosition,
        Animator.MovementMethod movementMethod,
        @Nullable Consumer<IAnimatedBlockData> blockDataRotator)
        throws Exception;
}
