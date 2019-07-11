package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.events.IPCancellable;
import nl.pim16aap2.bigdoors.events.PEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an action that is going to be applied to a door.
 *
 * @author Pim
 */
public class DoorActionEvent extends PEvent implements IPCancellable
{
    private final long doorUID;
    private final DoorActionCause cause;
    private final DoorActionType actionType;
    private boolean isCancelled = false;
    private final UUID responsible;
    private final DoorBase doorBase;

    public DoorActionEvent(final long doorUID, final DoorActionCause cause, final DoorActionType actionType,
                           @NotNull UUID responsible, @NotNull DoorBase doorBase)
    {
        this.doorUID = doorUID;
        this.cause = cause;
        this.actionType = actionType;
        this.responsible = responsible;
        this.doorBase = doorBase;
    }

    /**
     * Get the UID of the door that is being requested.
     *
     * @return The UID of the door.
     */
    public long getDoorUID()
    {
        return doorUID;
    }

    /**
     * Get the {@link DoorBase} that the action will be applied on.
     *
     * @return
     */
    public DoorBase getDoorBase()
    {
        return doorBase;
    }

    /**
     * Get what caused the door action request to be created.
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
    public @Nullable UUID getResponsible()
    {
        return responsible;
    }

    /**
     * Get the type of action action requested.
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
    public void setCancelled(boolean cancel)
    {
        isCancelled = cancel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled()
    {
        return isCancelled;
    }
}
