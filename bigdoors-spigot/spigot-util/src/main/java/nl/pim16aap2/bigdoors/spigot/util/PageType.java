package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a page in a BigDoors GUI.
 */
public enum PageType
{
    /**
     * The GUI page does not belong to BigDoors.
     */
    NOTBIGDOORS(null),

    /**
     * Page showing a list of doors.
     */
    DOORLIST(Message.GUI_PAGE_DOORLIST),

    /**
     * Page showing the information of one specific door.
     */
    DOORINFO(Message.GUI_PAGE_SUBMENU),

    /**
     * Confirm deletion.
     */
    CONFIRMATION(Message.GUI_PAGE_CONFIRM),

    /**
     * A page with all types that can be created.
     */
    DOORCREATION(Message.GUI_PAGE_NEWDOORS),

    /**
     * Delete an owner.
     */
    REMOVEOWNER(Message.GUI_PAGE_REMOVEOWNER),

    ;

    /**
     * The {@link Message} associated with the name of the {@link PageType}.
     */
    private final Message message;

    PageType(final Message message)
    {
        this.message = message;
    }

    /**
     * Gets the {@link Message} associated with the name of the {@link PageType}.
     *
     * @param type The {@link PageType}.
     * @return The {@link Message} associated with the name of the {@link PageType}.
     */
    public static @NotNull Message getMessage(final @NotNull PageType type)
    {
        return type.message;
    }
}
