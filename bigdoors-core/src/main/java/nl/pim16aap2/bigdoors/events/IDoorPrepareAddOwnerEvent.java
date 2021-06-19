package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.jetbrains.annotations.NotNull;

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
    @NotNull DoorOwner getNewDoorOwner();
}
