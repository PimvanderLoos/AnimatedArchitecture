package nl.pim16aap2.bigdoors.events.dooraction;

import java.util.UUID;

import nl.pim16aap2.bigdoors.events.PCancellable;
import nl.pim16aap2.bigdoors.events.PEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an action that is requested of a door.
 *
 * @author Pim
 */
public class DoorActionRequestEvent extends PEvent implements PCancellable
{
    private final long doorUID;
    private final DoorActionCause cause;
    private final DoorActionType actionType;
    private boolean isCancelled = false;
    private final UUID initiator;
    private String name = null;

    public DoorActionRequestEvent(final long doorUID, final DoorActionCause cause, final DoorActionType actionType,
        @Nullable UUID initiator)
    {
        this.doorUID = doorUID;
        this.cause = cause;
        this.actionType = actionType;
        this.initiator = initiator;
    }

    public DoorActionRequestEvent(final long doorUID, final DoorActionCause cause, final DoorActionType actionType)
    {
        this(doorUID, cause, actionType, null);
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
     * Get what caused the door action request to be created.
     *
     * @return The cause of the door action request.
     */
    public DoorActionCause getCause()
    {
        return cause;
    }

    /**
     * If a player was the cause of this door action request, get that player.
     *
     * @return The UUID of the player that requested this door action, otherwise
     *         null.
     * @see DoorActionRequestEvent#getCause()
     */
    public @Nullable UUID getInitiator()
    {
        return initiator;
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
    public void setCancelled(boolean cancel)
    {
        this.isCancelled = cancel;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCancelled()
    {
        return isCancelled;
    }
}
