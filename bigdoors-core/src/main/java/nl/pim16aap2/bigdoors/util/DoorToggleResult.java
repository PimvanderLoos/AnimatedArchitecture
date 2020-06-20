package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

/**
 * Represent the possible outcomes of trying to toggle a door.
 *
 * @author Pim
 */
public enum DoorToggleResult
{
    /**
     * No issues were encountered; everything went fine.
     */
    SUCCESS(Message.EMPTY),

    /**
     * No doors were found, so none were toggled.
     */
    NODOORSFOUND(Message.ERROR_NODOORSFOUND),

    /**
     * The door could not be toggled because it is already 'busy': i.e. it is currently moving.
     */
    BUSY(Message.ERROR_DOORISBUSY),

    /**
     * The door could not be toggled because it is locked.
     */
    LOCKED(Message.ERROR_DOORISLOCKED),

    /**
     * Some undefined error occurred while attempting to toggle this door.
     */
    ERROR(Message.ERROR_TOGGLEFAILURE),

    /**
     * The door could not be toggled because it was cancelled.
     */
    CANCELLED(Message.ERROR_TOGGLECANCELLED),

    /**
     * The door exceeded the size limit.
     */
    TOOBIG(Message.ERROR_DOORTOOBIG),

    /**
     * The player who tried to toggle it or, if not present (e.g. when toggled via redstone), the original creator does
     * not have permission to open to toggle the door because they are not allowed to break blocks in the new location.
     * This happens when a compatibility hook interferes (e.g. WorldGuard).
     */
    NOPERMISSION(Message.ERROR_NOPERMISSIONFORLOCATION),

    /**
     * An attempt to toggle (or open/close) a door failed because it was obstructed.
     */
    OBSTRUCTED(Message.ERROR_DOORISOBSTRUCTED),

    /**
     * The door did not have enough space to move.
     */
    NODIRECTION(Message.ERROR_NOOPENDIRECTION),

    /**
     * The door could not be opened because it is already open.
     */
    ALREADYOPEN(Message.ERROR_DOORALREADYOPEN),

    /**
     * The door could not be closed because it is already closed.
     */
    ALREADYCLOSED(Message.ERROR_DOORALREADYCLOSED),

    /**
     * The door could not be toggled because its type was disabled at compile time.
     */
    TYPEDISABLED(Message.ERROR_DOORTYPEDISABLED),
    ;


    /**
     * The {@link Message} associated with the {@link DoorToggleResult}.
     */
    private final Message message;

    DoorToggleResult(@NotNull final Message message)
    {
        this.message = message;
    }

    /**
     * Get the Key for the translation of this {@link DoorToggleResult}.
     *
     * @param result The {@link DoorToggleResult}.
     * @return The Key for the translation of this {@link DoorToggleResult}.
     */
    @NotNull
    public static Message getMessage(final @NotNull DoorToggleResult result)
    {
        return result.message;
    }
}
