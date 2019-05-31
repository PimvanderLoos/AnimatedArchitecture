package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorType
{
    DOOR        (0, true, "-BD", "Door", "GENERAL.DOORTYPE.Door",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_HORIZONTAL}),


    DRAWBRIDGE  (1, true, "-DB", "Drawbridge", "GENERAL.DOORTYPE.Drawbridge",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL}),


    PORTCULLIS  (2, true, "-PC", "Portcullis", "GENERAL.DOORTYPE.Portcullis",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_VERTICAL, DoorAttribute.BLOCKSTOMOVE}),


    ELEVATOR    (3, true, "-EL", "Elevator", "GENERAL.DOORTYPE.Elevator",
                 DoorType.PORTCULLIS.attributes),


    SLIDINGDOOR (4, true, "-SD", "SlidingDoor", "GENERAL.DOORTYPE.SlidingDoor",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL, DoorAttribute.BLOCKSTOMOVE}),


    FLAG        (5, true, "-FL", "Flag", "GENERAL.DOORTYPE.Flag",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER}),


    GARAGEDOOR  (6, true, "-GD", "GarageDoor", "GENERAL.DOORTYPE.GarageDoor",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2}),

    WINDMILL    (7, true, "-WM", "Windmill", "GENERAL.DOORTYPE.Windmill",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER,
                                      DoorAttribute.DIRECTION_ROTATE_VERTICAL2}),

    REVOLVINGDOOR (8, true, "-RD", "RevolvingDoor", "GENERAL.DOORTYPE.RevolvingDoor",
                 DoorType.DOOR.attributes),
    ;

    private final int val;
    private final String flag;
    private final String codeName; // Name used in code for various purposes (e.g. config).
    private final String nameKey; // Name used in messages
    private final boolean enabled;
    private final static Map<Integer, DoorType> valMap = new HashMap<>();
    private final static Map<String, DoorType> flagMap = new HashMap<>();
    private final DoorAttribute[] attributes;

    private DoorType(final int val, final boolean enabled, final String flag, final String codeName, final String nameKey,
        final DoorAttribute... attributes)
    {
        this.val = val;
        this.enabled = enabled;
        this.flag = flag;
        this.codeName = codeName;
        this.nameKey = nameKey;
        this.attributes = attributes;
    }

    public static int getValue(DoorType type)
    {
        return type.val;
    }

    public static String getNameKey(DoorType type)
    {
        return type.nameKey;
    }

    public static String getCodeName(DoorType type)
    {
        return type.codeName;
    }

    public static DoorType valueOf(int type)
    {
        return valMap.get(type);
    }

    public static DoorType valueOfFlag(String flag)
    {
        return flagMap.get(flag);
    }

    public static DoorAttribute[] getAttributes(DoorType type)
    {
        return type.attributes;
    }

    public static boolean isEnabled(DoorType type)
    {
        return type.enabled;
    }

    static
    {
        for (DoorType type : DoorType.values())
        {
            valMap.put(type.val, type);
            flagMap.put(type.flag, type);
        }
    }
}