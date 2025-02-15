package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.Structure;

/**
 * Represents the event where a structure will be created.
 */
public interface IStructurePrepareCreateEvent extends IStructureEvent, ICancellableAnimatedArchitectureEvent
{
    /**
     * Gets the {@link Structure} that was created.
     * <p>
     * Note that this is NOT the final {@link Structure} that will exist after creation; it is merely a
     * preview!
     *
     * @return The {@link Structure} that will be created.
     */
    @Override
    Structure getStructure();
}
