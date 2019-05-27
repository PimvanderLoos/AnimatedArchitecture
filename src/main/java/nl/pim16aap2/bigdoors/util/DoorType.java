package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorType
{
    DOOR        (0, "-BD", "GENERAL.DOORTYPE.Door",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_HORIZONTAL}),


    DRAWBRIDGE  (1, "-DB", "GENERAL.DOORTYPE.DrawBridge",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL}),


    PORTCULLIS  (2, "-PC", "GENERAL.DOORTYPE.Portcullis",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_VERTICAL, DoorAttribute.BLOCKSTOMOVE}),


    ELEVATOR    (3, "-EL", "GENERAL.DOORTYPE.Elevator",
                 DoorType.PORTCULLIS.attributes),


    SLIDINGDOOR (4, "-SD", "GENERAL.DOORTYPE.SlidingDoor",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL, DoorAttribute.BLOCKSTOMOVE}),


    FLAG        (5, "-FL", "GENERAL.DOORTYPE.Flag",
                 new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                      DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                      DoorAttribute.REMOVEOWNER});

    private int    val;
    private String flag;
    private String nameKey;
    private static Map<Integer, DoorType> valMap  = new HashMap<>();
    private static Map<String,  DoorType> flagMap = new HashMap<>();
    private DoorAttribute[] attributes;

    private DoorType(int val, String flag, String nameKey, DoorAttribute... attributes)
    {
        this.val = val;
        this.flag = flag;
        this.nameKey = nameKey;
        this.attributes = attributes;
    }

    public static int getValue (DoorType type)
    {
        return type.val;
    }

    public static String getNameKey (DoorType type)
    {
        return type.nameKey;
    }

    public static DoorType valueOf (int type)
    {
        return valMap.get(type);
    }

    public static DoorType valueOfFlag (String flag)
    {
        return flagMap.get(flag);
    }

    public static DoorAttribute[] getAttributes (DoorType type)
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