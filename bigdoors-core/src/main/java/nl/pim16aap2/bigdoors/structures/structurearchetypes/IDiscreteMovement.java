package nl.pim16aap2.bigdoors.structures.structurearchetypes;

import nl.pim16aap2.bigdoors.structures.AbstractStructure;

/**
 * Describes a type of structure that moves a certain number of blocks to open.
 *
 * @author Pim
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
