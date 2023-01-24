package nl.pim16aap2.bigdoors.movable.movablearchetypes;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

/**
 * Describes a type of movable that moves a certain number of blocks to open.
 *
 * @author Pim
 */
public interface IDiscreteMovement
{
    /**
     * Get the number of blocks this {@link AbstractMovable} will try to move. As explained at
     * {@link #setBlocksToMove(int)}, the {@link AbstractMovable} is not guaranteed to move as far as specified.
     *
     * @return The number of blocks the {@link AbstractMovable} will try to move.
     */
    int getBlocksToMove();

    /**
     * Change the number of blocks this {@link AbstractMovable} will try to move when opened. Note that this is only a
     * suggestion. It will never move more blocks than possible. Values less than 1 will use the default value for this
     * {@link AbstractMovable}.
     *
     * @param newBTM
     *     The number of blocks the {@link AbstractMovable} will try to move.
     */
    void setBlocksToMove(int newBTM);
}
