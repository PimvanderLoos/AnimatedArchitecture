package nl.pim16aap2.bigdoors.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;

import java.util.Optional;

/**
 * Represents the event where a door will be (un)locked.
 *
 * @author Pim
 */
public interface IDoorPrepareLockChangeEvent extends IPEvent, IPCancellable
{
    /**
     * Gets the {@link AbstractDoorBase} whose lock status will be changed.
     * <p>
     * Note that this has not been applied yet at this stage.
     *
     * @return The {@link AbstractDoorBase} whose lock status will be changed.
     */
    @NonNull AbstractDoorBase getDoor();

    /**
     * Gets the {@link IPPlayer} that was responsible for the creation this door.
     *
     * @return The {@link IPPlayer} that created if the creation was requested by a player. If it was requested by
     * something else (e.g. the server), an empty optional is returned.
     */
    @NonNull Optional<IPPlayer> getResponsible();

    /**
     * The new lock status of the {@link AbstractDoorBase} that will be applied if this event is not cancelled.
     *
     * @return The new lock status of the {@link AbstractDoorBase}, where true indicates locked, and false indicates
     * unlocked.
     */
    boolean newLockStatus();
}
