package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.movable.MovableOwner;

/**
 * Represents the event where an owner is removed from a movable.
 *
 * @author Pim
 */
public interface IMovablePrepareRemoveOwnerEvent extends IMovableEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the {@link MovableOwner} that will be removed from the movable.
     *
     * @return The {@link MovableOwner}.
     */
    MovableOwner getRemovedMovableOwner();
}
