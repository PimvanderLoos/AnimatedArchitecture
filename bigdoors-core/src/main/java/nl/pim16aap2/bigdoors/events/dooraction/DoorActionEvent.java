package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.events.IPCancellable;
import nl.pim16aap2.bigdoors.events.PEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents an action that is going to be applied to a door.
 *
 * @author Pim
 */
public class DoorActionEvent extends PEvent implements IPCancellable
{
    /**
     * The UID of the door this action will be applied to.
     */
    private final long doorUID;
    /**
     * What initiated this DoorAction event.
     */
    private final DoorActionCause cause;
    private final DoorActionType actionType;
    private final Optional<UUID> responsible;
    private final DoorBase doorBase;
    private boolean isCancelled = false;

    public DoorActionEvent(final long doorUID, final @NotNull DoorActionCause cause,
                           final @NotNull DoorActionType actionType, @Nullable final UUID responsible,
                           final @NotNull DoorBase doorBase)
    {
        this.doorUID = doorUID;
        this.cause = cause;
        this.actionType = actionType;
        this.responsible = Optional.ofNullable(responsible);
        this.doorBase = doorBase;
    }

    /**
     * Gets the UID of the door that is being requested.
     *
     * @return The UID of the door.
     */
    public long getDoorUID()
    {
        return doorUID;
    }

    /**
     * Gets the {@link DoorBase} that the action will be applied to.
     *
     * @return The {@link DoorBase} that the action will be applied to.
     */
    public DoorBase getDoorBase()
    {
        return doorBase;
    }

    /**
     * Gets what caused the door action request to be created.
     *
     * @return The cause of the door action request.
     */
    public DoorActionCause getCause()
    {
        return cause;
    }

    /**
     * Gets the UUID of the player responsible for this door action. This either means the player who directly requested
     * this action or, if it was requested indirectly, the original creator of the door.
     *
     * @return The UUID of the player that is responsible for this door.
     *
     * @see DoorActionRequestEvent#getCause()
     */
    public Optional<UUID> getResponsible()
    {
        return responsible;
    }

    /**
     * Gets the type of action action requested.
     *
     * @return The type of the requested action.
     */
    public DoorActionType getActionType()
    {
        return actionType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled()
    {
        return isCancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCancelled(boolean cancel)
    {
        isCancelled = cancel;
    }
}
