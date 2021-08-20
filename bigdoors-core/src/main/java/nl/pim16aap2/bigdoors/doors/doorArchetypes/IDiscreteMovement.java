package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.DoorBase;

/**
 * Describes a type of door that moves a certain number of blocks to open.
 *
 * @author Pim
 */
public interface IDiscreteMovement
{
    /**
     * Get the number of blocks this {@link DoorBase} will try to move. As explained at {@link #setBlocksToMove(int)},
     * the {@link DoorBase} is not guaranteed to move as far as specified.
     *
     * @return The number of blocks the {@link DoorBase} will try to move.
     */
    int getBlocksToMove();

    /**
     * Change the number of blocks this {@link DoorBase} will try to move when opened. Note that this is only a
     * suggestion. It will never move more blocks than possible. Values less than 1 will use the default value for this
     * {@link DoorBase}.
     *
     * @param newBTM The number of blocks the {@link DoorBase} will try to move.
     */
    void setBlocksToMove(int newBTM);
}
