package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.structures.StructureOwner;

/**
 * Represents the event where a new owner is added to a structure.
 *
 * @author Pim
 */
public interface IStructurePrepareAddOwnerEvent extends ICancellableBigDoorsEvent
{
    /**
     * Gets the new {@link StructureOwner} that will be added to the structure.
     *
     * @return The new {@link StructureOwner}.
     */
    StructureOwner getNewStructureOwner();
}
