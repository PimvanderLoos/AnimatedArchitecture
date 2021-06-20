package nl.pim16aap2.bigDoors.util;

import java.util.HashMap;

public enum PageType
{
    NOTBIGDOORS  (""),                     // This PageType does not belong to this plugin!
    DOORLIST     ("GUI.NAME.DoorList"   ), // Page showing a list of doors.
    DOORINFO     ("GUI.NAME.SubMenu"    ), // Page showing the information of one specific door.
    CONFIRMATION ("GUI.NAME.ConfirmMenu"), // Confirm delete.
    DOORCREATION ("GUI.NAME.NewDoors"   ), // Create new doors and such.
    REMOVEOWNER  ("GUI.NAME.REMOVEOWNER"); // Delete other owners.

    private String message;
    private static HashMap<String, PageType> map = new HashMap<>();

    private PageType(String message)
    {
        this.message = message;
    }

    public static String getMessage(PageType type)
    {
        return type.message;
    }

    public static PageType valueOfName(String str)
    {
        if (map.containsKey(str))
            return map.get(str);
        return PageType.NOTBIGDOORS;
    }

    static
    {
        for (PageType type : PageType.values())
            map.put(type.message, type);
    }
}
