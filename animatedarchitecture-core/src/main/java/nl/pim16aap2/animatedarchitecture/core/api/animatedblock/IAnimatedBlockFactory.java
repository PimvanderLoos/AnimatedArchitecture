package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a IFactory for {@link IAnimatedBlock} instances.
 *
 * @author Pim
 */
public interface IAnimatedBlockFactory
{
    /**
     * Creates a new {@link IAnimatedBlock} at the given location made of the provided block.
     *
     * @param world
     *     The world in which the animated block exists.
     * @param startPosition
     *     The starting position of the animated block.
     * @param radius
     *     The radius of the block to the rotation point.
     * @param onEdge
     *     True if this animated block is on the edge of the cuboid being animated.
     * @param finalPosition
     *     The final position of the block. This is the place where the block will be placed after the animation
     *     finishes.
     * @param blockDataRotator
     *     The consumer used to rotate the block data of the animated block. May be null to disable this.
     * @return The {@link IAnimatedBlock} that was constructed if it could be constructed.
     */
    Optional<IAnimatedBlock> create(
        IWorld world,
        RotatedPosition startPosition,
        float radius,
        boolean onEdge,
        RotatedPosition finalPosition,
        @Nullable Consumer<IAnimatedBlockData> blockDataRotator);
}
