package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.structures.StructureOwner;

/**
 * Represents the event where an owner is removed from a structure.
 *
 * @author Pim
 */
public interface IStructurePrepareRemoveOwnerEvent extends IStructureEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the {@link StructureOwner} that will be removed from the structure.
     *
     * @return The {@link StructureOwner}.
     */
    StructureOwner getRemovedStructureOwner();
}
