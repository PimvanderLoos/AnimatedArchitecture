package nl.pim16aap2.bigdoors.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorOwner;

import java.util.Optional;

/**
 * Represents the event where an owner is removed from a door.
 *
 * @author Pim
 */
public interface IDoorPrepareRemoveOwnerEvent extends IPEvent, IPCancellable
{
    /**
     * Gets the {@link AbstractDoorBase} for which an owner will be removed.
     *
     * @return The {@link AbstractDoorBase} for which an owner will be removed.
     */
    @NonNull AbstractDoorBase getDoor();

    /**
     * Gets the {@link IPPlayer} that was responsible for removing the owner from this door.
     *
     * @return The {@link IPPlayer} that removed the owner. If it was done by something else (e.g. the server), an empty
     * optional is returned.
     */
    @NonNull Optional<IPPlayer> getResponsible();

    /**
     * Gets the {@link DoorOwner} that will be removed from the door.
     *
     * @return The {@link DoorOwner}.
     */
    @NonNull DoorOwner getNewDoorOwner();
}
