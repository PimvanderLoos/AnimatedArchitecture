package nl.pim16aap2.bigdoors.spigotutil;

import nl.pim16aap2.bigdoors.util.messages.Message;

import java.util.HashMap;
import java.util.Map;

public enum PageType
{
    NOTBIGDOORS(null),                     // This PageType does not belong to this plugin!
    DOORLIST(Message.GUI_PAGE_DOORLIST), // Page showing a list of doors.
    DOORINFO(Message.GUI_PAGE_SUBMENU), // Page showing the information of one specific door.
    CONFIRMATION(Message.GUI_PAGE_CONFIRM), // Confirm delete.
    DOORCREATION(Message.GUI_PAGE_NEWDOORS), // Create new doors and such.
    REMOVEOWNER(Message.GUI_PAGE_REMOVEOWNER); // Delete other owners.

    private static Map<Message, PageType> map = new HashMap<>();

    static
    {
        for (PageType type : PageType.values())
            map.put(type.message, type);
    }

    private Message message;

    PageType(final Message message)
    {
        this.message = message;
    }

    public static Message getMessage(PageType type)
    {
        return type.message;
    }

    public static PageType valueOfName(String str)
    {
        if (map.containsKey(str))
            return map.get(str);
        return PageType.NOTBIGDOORS;
    }
}
