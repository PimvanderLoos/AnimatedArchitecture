package nl.pim16aap2.bigdoors.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.DoorOwner;

/**
 * Represents the event where a new owner is added to a door.
 *
 * @author Pim
 */
public interface IDoorPrepareAddOwnerEvent extends ICancellableBigDoorsEvent
{
    /**
     * Gets the new {@link DoorOwner} that will be added to the door.
     *
     * @return The new {@link DoorOwner}.
     */
    @NonNull DoorOwner getNewDoorOwner();
}
