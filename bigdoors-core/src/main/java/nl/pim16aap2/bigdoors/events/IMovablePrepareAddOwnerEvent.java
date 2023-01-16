package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.movable.MovableOwner;

/**
 * Represents the event where a new owner is added to a movable.
 *
 * @author Pim
 */
public interface IMovablePrepareAddOwnerEvent extends ICancellableBigDoorsEvent
{
    /**
     * Gets the new {@link MovableOwner} that will be added to the movable.
     *
     * @return The new {@link MovableOwner}.
     */
    MovableOwner getNewMovableOwner();
}
