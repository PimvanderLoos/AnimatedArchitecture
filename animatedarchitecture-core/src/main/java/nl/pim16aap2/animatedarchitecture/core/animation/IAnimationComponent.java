package nl.pim16aap2.animatedarchitecture.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents a component of an animation.
 */
public interface IAnimationComponent
{
    /**
     * Calculates the final position from the original coordinates of the block that will be animated.
     *
     * @param xAxis
     *     The x-coordinate of the original location of the block.
     * @param yAxis
     *     The y-coordinate of the original location of the block.
     * @param zAxis
     *     The z-coordinate of the original location of the block.
     * @return The final position of an {@link IAnimatedBlock}.
     */
    RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis);

    /**
     * Calculates the starting position from the original coordinates of the block that will be animated.
     *
     * @param xAxis
     *     The x-coordinate of the original location of the block.
     * @param yAxis
     *     The y-coordinate of the original location of the block.
     * @param zAxis
     *     The z-coordinate of the original location of the block.
     * @return The starting position of an {@link IAnimatedBlock}.
     */
    default RotatedPosition getStartPosition(int xAxis, int yAxis, int zAxis)
    {
        return new RotatedPosition(new Vector3Dd(xAxis, yAxis, zAxis));
    }

    /**
     * Provides the consumer that applies the rotation to the {@link IAnimatedBlockData}.
     * <p>
     * When set to null (the default value), it does not do anything.
     *
     * @return The rotation function for the animated block data.
     */
    default @Nullable Consumer<IAnimatedBlockData> getBlockDataRotator()
    {
        return null;
    }

    /**
     * Runs some optional steps before the animation is started.
     *
     * @param animator
     *     The animator responsible for the animation.
     */
    default void prepareAnimation(IAnimator animator)
    {
    }

    /**
     * Runs a single step of the animation.
     *
     * @param animator
     *     The animator responsible for the animation.
     * @param animatedBlocks
     *     The animated blocks for which to execute the animation step.
     * @param ticks
     *     The number of ticks that have passed since the start of the animation.
     */
    void executeAnimationStep(IAnimator animator, Iterable<IAnimatedBlock> animatedBlocks, int ticks);

    /**
     * Gets the radius of a block at the given coordinates.
     *
     * @param xAxis
     *     The x coordinate.
     * @param yAxis
     *     The y coordinate.
     * @param zAxis
     *     The z coordinate.
     * @return The radius of a block at the given coordinates.
     */
    default float getRadius(int xAxis, int yAxis, int zAxis)
    {
        return -1;
    }
}
