package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;

/**
 * Represents the event where a new owner is added to a structure.
 */
public interface IStructurePrepareAddOwnerEvent extends ICancellableAnimatedArchitectureEvent
{
    /**
     * Gets the new {@link StructureOwner} that will be added to the structure.
     *
     * @return The new {@link StructureOwner}.
     */
    StructureOwner getNewStructureOwner();
}
