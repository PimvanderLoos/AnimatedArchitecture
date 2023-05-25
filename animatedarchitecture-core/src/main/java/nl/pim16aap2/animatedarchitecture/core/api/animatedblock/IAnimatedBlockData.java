package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;

/**
 * Represents the internal data of an animated block.
 * <p>
 * These data represent a representation of the block in the structure as it is in the world.
 */
@SuppressWarnings("unused")
public interface IAnimatedBlockData
{
    /**
     * Checks if this block can rotate.
     *
     * @return True if this block can rotate.
     */
    boolean canRotate();

    /**
     * Rotates this block in a provided {@link MovementDirection}.
     *
     * @param movementDirection
     *     The {@link MovementDirection} to rotate this block in.
     * @return True if the block was rotated.
     */
    default boolean rotateBlock(MovementDirection movementDirection)
    {
        return rotateBlock(movementDirection, 1);
    }

    /**
     * Rotates this block in a provided {@link MovementDirection}.
     *
     * @param movementDirection
     *     The {@link MovementDirection} to rotate this block in.
     * @param times
     *     The number times to apply the rotation.
     * @return True if the block was rotated.
     */
    boolean rotateBlock(MovementDirection movementDirection, int times);

    /**
     * @param loc
     *     The position where the block will be placed.
     */
    void putBlock(IVector3D loc);

    /**
     * Deletes the block at the original location.
     */
    void deleteOriginalBlock();

    /**
     * Called after all blocks in a structure have been removed for any potential post-processing.
     * <p>
     * Subclasses can override this method if they need to do any post-processing, such as updating the physics around
     * the edges of the empty space where the structure used to be.
     */
    default void postProcessStructureRemoval()
    {
    }
}
