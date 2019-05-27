package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorType
{
    DOOR        (0, "-BD", "door", "GENERAL.DOORTYPE.Door",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_HORIZONTAL}),


    DRAWBRIDGE  (1, "-DB", "drawbridge", "GENERAL.DOORTYPE.DrawBridge",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL}),


    PORTCULLIS  (2, "-PC", "portcullis", "GENERAL.DOORTYPE.Portcullis",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_VERTICAL, DoorAttribute.BLOCKSTOMOVE}),


    ELEVATOR    (3, "-EL", "elevator", "GENERAL.DOORTYPE.Elevator",
                 DoorType.PORTCULLIS.attributes),


    SLIDINGDOOR (4, "-SD", "slidingDoor", "GENERAL.DOORTYPE.SlidingDoor",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL, DoorAttribute.BLOCKSTOMOVE}),


    FLAG        (5, "-FL", "flag", "GENERAL.DOORTYPE.Flag",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER}),


    GARAGEDOOR  (6, "-GD", "garageDoor", "GENERAL.DOORTYPE.GarageDoor",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL}),

    WINDMILL    (7, "-WM", "windmill", "GENERAL.DOORTYPE.Windmill",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER,
                                      DoorAttribute.DIRECTION_ROTATE_VERTICAL}),
    ;

    private final int val;
    private final String flag;
    private final String codeName; // Name used in code for various purposes (e.g. config).
    private final String nameKey; // Name used in messages
    private final static Map<Integer, DoorType> valMap = new HashMap<>();
    private final static Map<String, DoorType> flagMap = new HashMap<>();
    private final DoorAttribute[] attributes;

    private DoorType(final int val, final String flag, final String codeName, final String nameKey,
        final DoorAttribute... attributes)
    {
        this.val = val;
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

    static
    {
        for (DoorType type : DoorType.values())
        {
            valMap.put(type.val, type);
            flagMap.put(type.flag, type);
        }
    }
}