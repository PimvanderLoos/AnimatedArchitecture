package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.structures.AbstractStructure;

/**
 * Represents the event where a structure will be (un)locked.
 *
 * @author Pim
 */
public interface IStructurePrepareLockChangeEvent extends IStructureEvent, ICancellableBigDoorsEvent
{
    /**
     * The new lock status of the {@link AbstractStructure} that will be applied if this event is not cancelled.
     *
     * @return The new lock status of the {@link AbstractStructure}, where true indicates locked, and false indicates
     * unlocked.
     */
    boolean newLockStatus();
}
