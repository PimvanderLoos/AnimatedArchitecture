package nl.pim16aap2.bigdoors.core.structures.structurearchetypes;

import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;

/**
 * Describes a type of structure that moves in a straight line, where the final position of each block in a structure
 * after an animation can be calculated by adding or subtracting the same 'distance' value (measured in blocks as
 * integer value) to its starting position. The dimension(s) along which this is applied is up to the structure type
 * itself to define.
 * <p>
 * An example of a structure type with discrete movement is a portcullis, as that only goes straight up and straight
 * down.
 */
public interface IDiscreteMovement
{
    /**
     * Get the number of blocks this {@link AbstractStructure} will try to move. As explained at
     * {@link #setBlocksToMove(int)}, the {@link AbstractStructure} is not guaranteed to move as far as specified.
     *
     * @return The number of blocks the {@link AbstractStructure} will try to move.
     */
    int getBlocksToMove();

    /**
     * Change the number of blocks this {@link AbstractStructure} will try to move when opened. Note that this is only a
     * suggestion. It will never move more blocks than possible. Values less than 1 will use the default value for this
     * {@link AbstractStructure}.
     *
     * @param newBTM
     *     The number of blocks the {@link AbstractStructure} will try to move.
     */
    void setBlocksToMove(int newBTM);
}
