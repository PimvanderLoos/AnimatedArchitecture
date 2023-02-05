package nl.pim16aap2.bigdoors.core.api.animatedblock;

import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;

/**
 * Represents an NMS block.
 *
 * @author Pim
 */
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
    boolean rotateBlock(MovementDirection movementDirection);

    /**
     * @param loc
     *     The position where the block will be placed.
     */
    void putBlock(IVector3D loc);

    /**
     * Deletes the block at the original location.
     *
     * @param applyPhysics
     *     True to apply physics when removing this block. When this is false, stuff like torches that are attached to
     *     this block will not be broken upon removal of this block.
     */
    void deleteOriginalBlock(boolean applyPhysics);
}
