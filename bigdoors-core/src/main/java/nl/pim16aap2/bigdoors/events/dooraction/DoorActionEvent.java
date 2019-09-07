package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.events.IPCancellable;
import nl.pim16aap2.bigdoors.events.PEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents an action that is going to be applied to a door.
 *
 * @author Pim
 */
public interface DoorActionEvent extends PEvent, IPCancellable
{
//    /**
//     * Gets the door that is to be toggled.
//     *
//     * @return The door.
//     */
//    @NotNull
//    CompletableFuture<Optional<DoorBase>> getFutureDoor();

    /**
     * Gets what caused the door action request to be created.
     *
     * @return The cause of the door action request.
     */
    @NotNull
    DoorActionCause getCause();

    /**
     * Gets the UUID of the player responsible for this door action. This either means the player who directly requested
     * this action or, if it was requested indirectly, the original creator of the door.
     *
     * @return The UUID of the player that is responsible for this door.
     */
    @NotNull
    Optional<UUID> getResponsible();

    /**
     * Gets the type of action action requested.
     *
     * @return The type of the requested action.
     */
    @NotNull
    DoorActionType getActionType();

    /**
     * Checks if the door should skip its animation and open instantly.
     *
     * @return True if the door should open instantly.
     */
    boolean getInstantOpen();

    /**
     * Gets the number of seconds the door will try to take to open.
     *
     * @return The number of seconds the door will try to take to open.
     */
    double getTime();

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isCancelled();

    /**
     * {@inheritDoc}
     */
    @Override
    void setCancelled(boolean cancel);
}
