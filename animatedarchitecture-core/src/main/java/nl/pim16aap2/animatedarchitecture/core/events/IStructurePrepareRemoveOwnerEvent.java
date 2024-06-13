package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;

/**
 * Represents the event where an owner is removed from a structure.
 */
public interface IStructurePrepareRemoveOwnerEvent extends IStructureEvent, ICancellableAnimatedArchitectureEvent
{
    /**
     * Gets the {@link StructureOwner} that will be removed from the structure.
     *
     * @return The {@link StructureOwner}.
     */
    StructureOwner getRemovedStructureOwner();
}
