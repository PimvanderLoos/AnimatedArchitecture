package nl.pim16aap2.bigdoors.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.DoorOwner;

/**
 * Represents the event where an owner is removed from a door.
 *
 * @author Pim
 */
public interface IDoorPrepareRemoveOwnerEvent extends IDoorEvent, IPCancellable
{
    /**
     * Gets the {@link DoorOwner} that will be removed from the door.
     *
     * @return The {@link DoorOwner}.
     */
    @NonNull DoorOwner getRemovedDoorOwner();
}
