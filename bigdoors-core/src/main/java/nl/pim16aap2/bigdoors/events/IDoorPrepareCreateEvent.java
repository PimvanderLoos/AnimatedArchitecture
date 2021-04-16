package nl.pim16aap2.bigdoors.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;

import java.util.Optional;

/**
 * Represents the event where a door will be created.
 *
 * @author Pim
 */
public interface IDoorPrepareCreateEvent extends IPEvent, IPCancellable
{
    /**
     * Gets the {@link AbstractDoorBase} that will be created.
     * <p>
     * Note that this is NOT the final {@link AbstractDoorBase} that will exist after creation; it is merely a preview!
     *
     * @return The {@link AbstractDoorBase} that will be created.
     */
    @NonNull AbstractDoorBase getDoor();

    /**
     * Gets the {@link IPPlayer} that was responsible for the creation this door.
     *
     * @return The {@link IPPlayer} that created if the creation was requested by a player. If it was requested by
     * something else (e.g. the server), an empty optional is returned.
     */
    @NonNull Optional<IPPlayer> getResponsible();
}
