package nl.pim16aap2.bigdoors.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;

import java.util.Optional;

public interface IDoorDeleteEvent extends IPEvent, IPCancellable
{
    /**
     * Gets the {@link AbstractDoorBase} that was requested to be deleted.
     *
     * @return The {@link AbstractDoorBase} that was requested to be deleted.
     */
    @NonNull AbstractDoorBase getDoor();

    /**
     * Gets the {@link IPPlayer} that was responsible for trying to delete this door.
     *
     * @return The {@link IPPlayer} that requested this door to be deleted if the deletion was requested by a player. If
     * it was requested by something else (e.g. the server), an empty optional is returned.
     */
    @NonNull Optional<IPPlayer> getResponsible();
}
