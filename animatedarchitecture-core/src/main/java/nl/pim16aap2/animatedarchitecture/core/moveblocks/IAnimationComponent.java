package nl.pim16aap2.animatedarchitecture.core.moveblocks;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

/**
 * Represents a component of an animation.
 */
public interface IAnimationComponent
{
    /**
     * @param startLocation
     *     The start location of a block.
     * @param radius
     *     The radius of the block to the rotation point.
     * @return The final position of an {@link IAnimatedBlock}.
     */
    Vector3Dd getFinalPosition(IVector3D startLocation, float radius);

    /**
     * @param startLocation
     *     The start location of a block.
     * @param radius
     *     The radius of the block to the rotation point.
     * @return The final position of an {@link IAnimatedBlock}.
     */
    default RotatedPosition getFinalPosition0(IVector3D startLocation, float radius)
    {
        return new RotatedPosition(getFinalPosition(startLocation, radius));
    }

    default RotatedPosition getStartPosition(int xAxis, int yAxis, int zAxis)
    {
        return new RotatedPosition(new Vector3Dd(xAxis + 0.5, yAxis, zAxis + 0.5));
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

    Vector3Dd getRotationPoint();

    /**
     * Runs a single step of the animation.
     *
     * @param animator
     *     The animator responsible for the animation.
     * @param ticks
     *     The number of ticks that have passed since the start of the animation.
     * @param ticksRemaining
     *     The number of ticks remaining in the animation.
     */
    void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining);

    /**
     * @return The type of movement to apply to animated blocks.
     * <p>
     * Subclasses are free to override this if a different type of movement is desired for that type.
     * <p>
     * Each animated block is moved using {@link Animator.MovementMethod#apply(IAnimatedBlock, Vector3Dd, int)}.
     */
    default Animator.MovementMethod getMovementMethod()
    {
        return Animator.MovementMethod.TELEPORT_VELOCITY;
    }

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

    /**
     * Gets the starting angle of a block (in rads) at the given coordinates.
     *
     * @param xAxis
     *     The x coordinate.
     * @param yAxis
     *     The y coordinate.
     * @param zAxis
     *     The z coordinate.
     * @return The starting angle of a block at the given coordinates.
     */
    default float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return -1;
    }
}
