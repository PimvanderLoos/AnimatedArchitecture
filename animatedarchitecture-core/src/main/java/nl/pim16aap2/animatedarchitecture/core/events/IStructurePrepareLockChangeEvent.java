package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;

/**
 * Represents the event where a structure will be (un)locked.
 */
public interface IStructurePrepareLockChangeEvent extends IStructureEvent, ICancellableAnimatedArchitectureEvent
{
    /**
     * The new lock status of the {@link AbstractStructure} that will be applied if this event is not cancelled.
     *
     * @return The new lock status of the {@link AbstractStructure}, where true indicates locked, and false indicates
     * unlocked.
     */
    boolean newLockStatus();
}
