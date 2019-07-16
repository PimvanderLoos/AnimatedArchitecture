package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;

import java.util.HashMap;
import java.util.Map;

public enum DoorType
{
    BIGDOOR(0, true, "-BD", Message.DOORTYPE_BIGDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
            DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_HORIZONTAL)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new BigDoor(plugin, doorUID);
                }
            },

    DRAWBRIDGE(1, true, "-DB", Message.DOORTYPE_DRAWBRIDGE, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new Drawbridge(plugin, doorUID);
                }
            },

    PORTCULLIS(2, true, "-PC", Message.DOORTYPE_PORTCULLIS, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_VERTICAL,
               DoorAttribute.BLOCKSTOMOVE)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new Portcullis(plugin, doorUID);
                }
            },

    ELEVATOR(3, true, "-EL", Message.DOORTYPE_ELEVATOR, DoorType.PORTCULLIS.attributes)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new Elevator(plugin, doorUID);
                }
            },

    SLIDINGDOOR(4, true, "-SD", Message.DOORTYPE_SLIDINGDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
                DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
                DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL,
                DoorAttribute.BLOCKSTOMOVE)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new SlidingDoor(plugin, doorUID);
                }
            },

    FLAG(5, true, "-FL", Message.DOORTYPE_FLAG, DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO,
         DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
         DoorAttribute.REMOVEOWNER)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new Flag(plugin, doorUID);
                }
            },

    GARAGEDOOR(6, true, "-GD", Message.DOORTYPE_GARAGEDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new GarageDoor(plugin, doorUID);
                }
            },

    WINDMILL(7, true, "-WM", Message.DOORTYPE_WINDMILL, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
             DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER,
             DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new Windmill(plugin, doorUID);
                }
            },

    REVOLVINGDOOR(8, true, "-RD", Message.DOORTYPE_REVOLVINGDOOR, DoorType.BIGDOOR.attributes)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new RevolvingDoor(plugin, doorUID);
                }
            },

    CLOCK(7, true, "-CL", Message.DOORTYPE_CLOCK, DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO,
          DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER,
          DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
            {
                @Override
                public DoorBase getNewDoor(final BigDoors plugin, final long doorUID)
                {
                    return new Windmill(plugin, doorUID);
                }
            },
    ;

    private final static Map<Integer, DoorType> valMap = new HashMap<>();
    private final static Map<String, DoorType> flagMap = new HashMap<>();

    static
    {
        for (DoorType type : DoorType.values())
        {
            valMap.put(type.val, type);
            flagMap.put(type.flag, type);
        }
    }

    private final int val;
    private final String flag;
    private final Message message;
    private final boolean enabled;
    private final DoorAttribute[] attributes;

    DoorType(final int val, final boolean enabled, final String flag, final Message message,
             final DoorAttribute... attributes)
    {
        this.val = val;
        this.enabled = enabled;
        this.flag = flag;
        this.message = message;
        this.attributes = attributes;
    }

    public static int getValue(DoorType type)
    {
        return type.val;
    }

    public static Message getMessage(DoorType type)
    {
        return type.message;
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

    public abstract DoorBase getNewDoor(final BigDoors plugin, final long doorUID);
}
