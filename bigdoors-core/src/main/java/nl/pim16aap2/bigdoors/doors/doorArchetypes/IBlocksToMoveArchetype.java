package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.IDoorBase;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a type of door that moves a certain number of blocks to open.
 * <p>
 * TODO: Give this Archetype a less awful name.
 *
 * @author Pim
 */
public interface IBlocksToMoveArchetype extends IDoorBase
{
    /**
     * Get the number of blocks this {@link AbstractDoorBase} will try to move. As explained at {@link
     * #setBlocksToMove(int)}, the {@link AbstractDoorBase} is not guaranteed to move as far as specified.
     *
     * @return The number of blocks the {@link AbstractDoorBase} will try to move.
     */
    int getBlocksToMove();

    /**
     * Change the number of blocks this {@link AbstractDoorBase} will try to move when opened. Note that this is only a
     * suggestion. It will never move more blocks than possible. Values less than 1 will use the default value for this
     * {@link AbstractDoorBase}.
     *
     * @param newBTM The number of blocks the {@link AbstractDoorBase} will try to move.
     * @return The instance of this door.
     */
    @NotNull AbstractDoorBase setBlocksToMove(final int newBTM);
}
