package nl.pim16aap2.bigdoors.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorOwner;

import java.util.Optional;

/**
 * Represents the event where a new owner is added to a door.
 *
 * @author Pim
 */
public interface IDoorPrepareAddOwnerEvent extends IPEvent, IPCancellable
{
    /**
     * Gets the {@link AbstractDoorBase} for which an owner will be added.
     *
     * @return The {@link AbstractDoorBase} for which an owner will be added.
     */
    @NonNull AbstractDoorBase getDoor();

    /**
     * Gets the {@link IPPlayer} that was responsible for adding a new owner to this door.
     *
     * @return The {@link IPPlayer} that added the new owner. If it was done by something else (e.g. the server), an
     * empty optional is returned.
     */
    @NonNull Optional<IPPlayer> getResponsible();

    /**
     * Gets the new {@link DoorOwner} that will be added to the door.
     *
     * @return The new {@link DoorOwner}.
     */
    @NonNull DoorOwner getNewDoorOwner();
}
