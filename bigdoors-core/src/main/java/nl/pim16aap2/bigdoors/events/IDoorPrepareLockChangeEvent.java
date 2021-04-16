package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;

/**
 * Represents the event where a door will be (un)locked.
 *
 * @author Pim
 */
public interface IDoorPrepareLockChangeEvent extends IDoorEvent, ICancellableBigDoorsEvent
{
    /**
     * The new lock status of the {@link AbstractDoorBase} that will be applied if this event is not cancelled.
     *
     * @return The new lock status of the {@link AbstractDoorBase}, where true indicates locked, and false indicates
     * unlocked.
     */
    boolean newLockStatus();
}
