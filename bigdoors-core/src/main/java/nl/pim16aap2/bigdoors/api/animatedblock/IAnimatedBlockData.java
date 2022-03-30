package nl.pim16aap2.bigdoors.api.animatedblock;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.util.RotateDirection;

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
     * Rotates this block in a provided {@link RotateDirection}.
     *
     * @param rotDir
     *     The {@link RotateDirection} to rotate this block in.
     */
    void rotateBlock(RotateDirection rotDir);

    /**
     * Places the block at a given location.
     *
     * @param loc
     *     The location where the block will be placed.
     */
    void putBlock(IPLocation loc);

    /**
     * Deletes the block at the original location.
     */
    void deleteOriginalBlock();
}
