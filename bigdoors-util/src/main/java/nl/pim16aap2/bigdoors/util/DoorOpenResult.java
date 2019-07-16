package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.messages.Message;

/**
 * Represent the possible outcomes of trying to open a door.
 *
 * @author Pim
 */
public enum DoorOpenResult
{
    SUCCESS(null),
    BUSY(Message.ERROR_DOORISBUSY),
    LOCKED(Message.ERROR_DOORISLOCKED),
    ERROR(Message.ERROR_TOGGLEFAILURE),
    NOPERMISSION(Message.ERROR_NOPERMISSIONFORLOCATION),
    NODIRECTION(Message.ERROR_NOOPENDIRECTION),
    ALREADYOPEN(Message.ERROR_DOORALREADYOPEN),
    ALREADYCLOSED(Message.ERROR_DOORALREADYCLOSED),
    TYPEDISABLED(Message.ERROR_DOORTYPEDISABLED);

    private Message message;

    DoorOpenResult(Message message)
    {
        this.message = message;
    }

    /**
     * Get the Key for the translation of this {@link DoorOpenResult}.
     *
     * @param result The {@link DoorOpenResult}.
     * @return The Key for the translation of this {@link DoorOpenResult}.
     */
    public static Message getMessage(DoorOpenResult result)
    {
        return result.message;
    }
}
