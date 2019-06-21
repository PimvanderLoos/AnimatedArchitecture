package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.spigotutil.DoorAttribute;

import java.util.HashMap;
import java.util.Map;

public enum DoorType
{
    BIGDOOR (0, true, "-BD", "BigDoor", "GENERAL.DOORTYPE.BigDoor", DoorAttribute.LOCK,
            DoorAttribute.TOGGLE,
            DoorAttribute.INFO,
            DoorAttribute.DELETE,
            DoorAttribute.RELOCATEPOWERBLOCK,
            DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER,
            DoorAttribute.REMOVEOWNER,
            DoorAttribute.DIRECTION_ROTATE_HORIZONTAL)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new BigDoor(plugin, doorUID);
        }
    },

    DRAWBRIDGE (1, true, "-DB", "Drawbridge", "GENERAL.DOORTYPE.Drawbridge", DoorAttribute.LOCK,
            DoorAttribute.TOGGLE,
            DoorAttribute.INFO,
            DoorAttribute.DELETE,
            DoorAttribute.RELOCATEPOWERBLOCK,
            DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER,
            DoorAttribute.REMOVEOWNER,
            DoorAttribute.DIRECTION_ROTATE_VERTICAL)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new Drawbridge(plugin, doorUID);
        }
    },

    PORTCULLIS (2, true, "-PC", "Portcullis", "GENERAL.DOORTYPE.Portcullis", DoorAttribute.LOCK,
            DoorAttribute.TOGGLE,
            DoorAttribute.INFO,
            DoorAttribute.DELETE,
            DoorAttribute.RELOCATEPOWERBLOCK,
            DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER,
            DoorAttribute.REMOVEOWNER,
            DoorAttribute.DIRECTION_STRAIGHT_VERTICAL,
            DoorAttribute.BLOCKSTOMOVE)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new Portcullis(plugin, doorUID);
        }
    },

    ELEVATOR (3, true, "-EL", "Elevator", "GENERAL.DOORTYPE.Elevator", DoorType.PORTCULLIS.attributes)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new Elevator(plugin, doorUID);
        }
    },

    SLIDINGDOOR (4, true, "-SD", "SlidingDoor", "GENERAL.DOORTYPE.SlidingDoor", DoorAttribute.LOCK,
            DoorAttribute.TOGGLE,
            DoorAttribute.INFO,
            DoorAttribute.DELETE,
            DoorAttribute.RELOCATEPOWERBLOCK,
            DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER,
            DoorAttribute.REMOVEOWNER,
            DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL,
            DoorAttribute.BLOCKSTOMOVE)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new SlidingDoor(plugin, doorUID);
        }
    },

    FLAG (5, true, "-FL", "Flag", "GENERAL.DOORTYPE.Flag", DoorAttribute.LOCK,
            DoorAttribute.TOGGLE,
            DoorAttribute.INFO,
            DoorAttribute.DELETE,
            DoorAttribute.RELOCATEPOWERBLOCK,
            DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER,
            DoorAttribute.REMOVEOWNER)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new Flag(plugin, doorUID);
        }
    },

    GARAGEDOOR (6, true, "-GD", "GarageDoor", "GENERAL.DOORTYPE.GarageDoor", DoorAttribute.LOCK,
            DoorAttribute.TOGGLE,
            DoorAttribute.INFO,
            DoorAttribute.DELETE,
            DoorAttribute.RELOCATEPOWERBLOCK,
            DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER,
            DoorAttribute.REMOVEOWNER,
            DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new GarageDoor(plugin, doorUID);
        }
    },

    WINDMILL (7, true, "-WM", "Windmill", "GENERAL.DOORTYPE.Windmill", DoorAttribute.LOCK,
            DoorAttribute.TOGGLE,
            DoorAttribute.INFO,
            DoorAttribute.DELETE,
            DoorAttribute.RELOCATEPOWERBLOCK,
            DoorAttribute.ADDOWNER,
            DoorAttribute.REMOVEOWNER,
            DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new Windmill(plugin, doorUID);
        }
    },

    REVOLVINGDOOR (8, true, "-RD", "RevolvingDoor", "GENERAL.DOORTYPE.RevolvingDoor", DoorType.BIGDOOR.attributes)
    {
        @Override
        public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
        {
            return new RevolvingDoor(plugin, doorUID);
        }
    },;

    private final int val;
    private final String flag;
    private final String codeName; // Name used in code for various purposes (e.g. config).
    private final String nameKey; // Name used in messages
    private final boolean enabled;
    private final static Map<Integer, DoorType> valMap = new HashMap<>();
    private final static Map<String, DoorType> flagMap = new HashMap<>();
    private final DoorAttribute[] attributes;

    DoorType(final int val, final boolean enabled, final String flag, final String codeName,
             final String nameKey, final DoorAttribute... attributes)
    {
        this.val = val;
        this.enabled = enabled;
        this.flag = flag;
        this.codeName = codeName;
        this.nameKey = nameKey;
        this.attributes = attributes;
    }

    public abstract DoorBase getNewDoor(final BigDoors plugin, final long doorUID);

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