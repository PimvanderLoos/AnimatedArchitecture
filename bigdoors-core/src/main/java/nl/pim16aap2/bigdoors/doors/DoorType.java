package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a type of door.
 *
 * @author Pim
 */
public enum DoorType
{
    BIGDOOR(0, true, "-BD", Message.DOORTYPE_BIGDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
            DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_HORIZONTAL)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new BigDoor(pLogger, doorUID, doorData);
            }
        },

    DRAWBRIDGE(1, true, "-DB", Message.DOORTYPE_DRAWBRIDGE, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new Drawbridge(pLogger, doorUID, doorData);
            }
        },

    PORTCULLIS(2, true, "-PC", Message.DOORTYPE_PORTCULLIS, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_VERTICAL,
               DoorAttribute.BLOCKSTOMOVE)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new Portcullis(pLogger, doorUID, doorData);
            }
        },

    ELEVATOR(3, true, "-EL", Message.DOORTYPE_ELEVATOR, DoorType.PORTCULLIS.attributes)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new Elevator(pLogger, doorUID, doorData);
            }
        },

    SLIDINGDOOR(4, true, "-SD", Message.DOORTYPE_SLIDINGDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
                DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
                DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL,
                DoorAttribute.BLOCKSTOMOVE)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new SlidingDoor(pLogger, doorUID, doorData);
            }
        },

    FLAG(5, true, "-FL", Message.DOORTYPE_FLAG, DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO,
         DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
         DoorAttribute.REMOVEOWNER)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new Flag(pLogger, doorUID, doorData);
            }
        },

    GARAGEDOOR(6, true, "-GD", Message.DOORTYPE_GARAGEDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new GarageDoor(pLogger, doorUID, doorData);
            }
        },

    WINDMILL(7, true, "-WM", Message.DOORTYPE_WINDMILL, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
             DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER,
             DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new Windmill(pLogger, doorUID, doorData);
            }
        },

    REVOLVINGDOOR(8, true, "-RD", Message.DOORTYPE_REVOLVINGDOOR, DoorType.BIGDOOR.attributes)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new RevolvingDoor(pLogger, doorUID, doorData);
            }
        },

    CLOCK(9, false, "-CL", Message.DOORTYPE_CLOCK, DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO,
          DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER,
          DoorAttribute.DIRECTION_ROTATE_VERTICAL2)
        {
            @NotNull
            @Override
            public DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                       final @NotNull DoorBase.DoorData doorData)
            {
                return new Windmill(pLogger, doorUID, doorData);
            }
        },
    ;

    private final static List<DoorType> cachedValues = Collections.unmodifiableList(Arrays.asList(DoorType.values()));
    private final static Map<Integer, DoorType> valMap = new HashMap<>();
    private final static Map<String, DoorType> commandFlagMap = new HashMap<>();

    static
    {
        for (DoorType type : DoorType.cachedValues())
        {
            valMap.put(type.val, type);
            commandFlagMap.put(type.commandFlag, type);
        }
    }

    /**
     * Gets the cached result of {@link #values()}.
     *
     * @return The cached array of values.
     */
    public static List<DoorType> cachedValues()
    {
        return cachedValues;
    }

    /**
     * The index value of this type.
     */
    private final int val;
    /**
     * The flag of this type (as used in commands).
     */
    private final String commandFlag;
    /**
     * The {@link Message} associated with the name of this type.
     */
    private final Message message;
    /**
     * Whether or not this type is enabled.
     */
    private final boolean enabled;
    /**
     * The attributes of this door. For example, a {@link DoorType#BIGDOOR} would have no use for {@link
     * DoorAttribute#BLOCKSTOMOVE} because it moves in quarter circles, not in numbers of blocks.
     */
    private final DoorAttribute[] attributes;

    DoorType(final int val, final boolean enabled, final @NotNull String commandFlag, final @NotNull Message message,
             final @NotNull DoorAttribute... attributes)
    {
        this.val = val;
        this.enabled = enabled;
        this.commandFlag = commandFlag;
        this.message = message;
        this.attributes = attributes;
    }

    /**
     * Gets the index value of a {@link DoorType}.
     *
     * @param type The {@link DoorType}.
     * @return The index value of a {@link DoorType}.
     */
    public static int getValue(final @NotNull DoorType type)
    {
        return type.val;
    }

    /**
     * Gets the {@link Message} associated with the name of a {@link DoorType}.
     *
     * @param type The {@link DoorType}.
     * @return The {@link Message} associated with the name of a {@link DoorType}.
     */
    @NotNull
    public static Message getMessage(final @NotNull DoorType type)
    {
        return type.message;
    }

    /**
     * Gets the {@link DoorType} from an index value.
     *
     * @param type The index value of this type.
     * @return The {@link DoorType} with this index value.
     */
    @NotNull
    public static DoorType valueOf(final int type) throws IllegalArgumentException
    {
        if (type >= valMap.size())
            throw new IllegalArgumentException(
                "Requested DoorType(" + type + "), but only " + valMap.size() + " are available!");
        return valMap.get(type);
    }

    /**
     * Gets the {@link DoorType} from its commandFlag, e.g. "-BD" for {@link DoorType#BIGDOOR}).
     *
     * @param commandFlag The command flag
     * @return The {@link DoorType} associated with this command flag or null if none was found.
     */
    public static Optional<DoorType> valueOfCommandFlag(final @NotNull String commandFlag)
    {
        return Optional.ofNullable(commandFlagMap.getOrDefault(commandFlag, null));
    }

    /**
     * Gets all {@link DoorAttribute}s of the {@link DoorType}.
     *
     * @param type The {@link DoorType}.
     * @return All {@link DoorAttribute}s of the {@link DoorType}.
     */
    @NotNull
    public static DoorAttribute[] getAttributes(final @NotNull DoorType type)
    {
        return type.attributes;
    }

    /**
     * Checks if a {@link DoorType} is enabled or not.
     *
     * @param type The {@link DoorType}.
     * @return True if a {@link DoorType} is enabled.
     */
    public static boolean isEnabled(final @NotNull DoorType type)
    {
        return type.enabled;
    }

    /**
     * Constructs a new {@link DoorBase} of this type.
     *
     * @param pLogger  The {@link PLogger} to be used for exception handling.
     * @param doorUID  The UID of the {@link DoorBase} to instantiate.
     * @param doorData The data required for basic door initialization.
     * @return A new {@link DoorBase} of this type.
     */
    @NotNull
    public abstract DoorBase getNewDoor(final @NotNull PLogger pLogger, final long doorUID,
                                        final @NotNull DoorBase.DoorData doorData);

    /**
     * Constructs a new {@link DoorBase} of a {@link DoorType}.
     *
     * @param type     The {@link DoorType} of the {@link DoorBase} to instantiate.
     * @param pLogger  The {@link PLogger} to be used for exception handling.
     * @param doorUID  The UID of the {@link DoorBase} to instantiate.
     * @param doorData The data required for basic door initialization.
     * @return A new {@link DoorBase} of a {@link DoorType}.
     */
    @NotNull
    public static DoorBase getNewDoor(final @NotNull DoorType type, final @NotNull PLogger pLogger,
                                      final @NotNull DoorBase.DoorData doorData, final long doorUID)
    {
        return type.getNewDoor(pLogger, doorUID, doorData);
    }
}
