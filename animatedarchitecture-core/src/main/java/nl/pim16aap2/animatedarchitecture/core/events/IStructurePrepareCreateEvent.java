package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;

/**
 * Represents the event where a structure will be created.
 */
public interface IStructurePrepareCreateEvent extends IStructureEvent, ICancellableAnimatedArchitectureEvent
{
    /**
     * Gets the {@link AbstractStructure} that was created.
     * <p>
     * Note that this is NOT the final {@link AbstractStructure} that will exist after creation; it is merely a
     * preview!
     *
     * @return The {@link AbstractStructure} that will be created.
     */
    @Override
    AbstractStructure getStructure();
}
