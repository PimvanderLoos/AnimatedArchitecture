package nl.pim16aap2.bigDoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorType
{
    // DoorType     // Value // Creator Flag // Translation key             // Perm
    DOOR           (0,       "-BD",          "GENERAL.DOORTYPE.Door"        , "bigdoors.user.createdoor.door",
                    new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                         DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                         DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_NSEW_LOOK}),


    DRAWBRIDGE     (1,       "-DB",          "GENERAL.DOORTYPE.DrawBridge"  , "bigdoors.user.createdoor.drawbridge",
                    new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                         DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                         DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_NSEW_LOOK}),


    PORTCULLIS     (2,       "-PC",          "GENERAL.DOORTYPE.Portcullis"  , "bigdoors.user.createdoor.portcullis",
                    new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                         DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                         DoorAttribute.REMOVEOWNER, DoorAttribute.BLOCKSTOMOVE}),


    ELEVATOR       (3,       "-EL",          "GENERAL.DOORTYPE.Elevator"    , "bigdoors.user.createdoor.elevator",
                    new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                         DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                         DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_GO, DoorAttribute.BLOCKSTOMOVE}),


    SLIDINGDOOR    (4,       "-SD",          "GENERAL.DOORTYPE.SlidingDoor" , "bigdoors.user.createdoor.slidingdoor",
                    new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                         DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                         DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_GO, DoorAttribute.BLOCKSTOMOVE}),


    FLAG           (5,       "-FL",          "GENERAL.DOORTYPE.Flag"        , "bigdoors.user.createdoor.flag",
                    new DoorAttribute[] {DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO, DoorAttribute.DELETE,
                                         DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
                                         DoorAttribute.REMOVEOWNER});

    private int    val;
    private String flag;
    private String nameKey;
    private String permission;
    private static Map<Integer, DoorType> valMap  = new HashMap<Integer, DoorType>();
    private static Map<String,  DoorType> flagMap = new HashMap<String,  DoorType>();
    private DoorAttribute[] attributes;

    private DoorType(int val, String flag, String nameKey, String permission, DoorAttribute... attributes)
    {
        this.val  = val;
        this.flag = flag;
        this.nameKey = nameKey;
        this.permission = permission;
        this.attributes = attributes;
    }

    public static int             getValue      (DoorType type) {  return type.val;           }
    public static String          getNameKey    (DoorType type) {  return type.nameKey;       }
    public static String          getPermission (DoorType type) {  return type.permission;    }
    public static DoorType        valueOf       (int type)      {  return valMap.get(type);   }
    public static DoorType        valueOfFlag   (String flag)   {  return flagMap.get(flag);  }
    public static DoorAttribute[] getAttributes (DoorType type) {  return type.attributes;    }

    static
    {
        for (DoorType type : DoorType.values())
        {
            valMap.put( type.val,  type);
            flagMap.put(type.flag, type);
        }
    }
}