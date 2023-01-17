package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

/**
 * Represents the event where a movable will be (un)locked.
 *
 * @author Pim
 */
public interface IMovablePrepareLockChangeEvent extends IMovableEvent, ICancellableBigDoorsEvent
{
    /**
     * The new lock status of the {@link AbstractMovable} that will be applied if this event is not cancelled.
     *
     * @return The new lock status of the {@link AbstractMovable}, where true indicates locked, and false indicates
     * unlocked.
     */
    boolean newLockStatus();
}
