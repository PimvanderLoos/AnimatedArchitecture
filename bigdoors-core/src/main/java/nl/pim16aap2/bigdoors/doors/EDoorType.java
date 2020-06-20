package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.DoorAttribute;
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
// Regex to quickly get a list of all types: \([\d\s\w\@\.\n\,\"\)\(\{\-\;]*\}[\s]*\}\,\n
public enum EDoorType
{
    BIGDOOR(0, true, "-BD", Message.DOORTYPE_BIGDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
            DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
            DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_HORIZONTAL),
    
    DRAWBRIDGE(1, true, "-DB", Message.DOORTYPE_DRAWBRIDGE, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2),

    PORTCULLIS(2, true, "-PC", Message.DOORTYPE_PORTCULLIS, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_VERTICAL,
               DoorAttribute.BLOCKSTOMOVE),

    ELEVATOR(3, true, "-EL", Message.DOORTYPE_ELEVATOR, EDoorType.PORTCULLIS.attributes),

    SLIDINGDOOR(4, true, "-SD", Message.DOORTYPE_SLIDINGDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
                DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
                DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL,
                DoorAttribute.BLOCKSTOMOVE),

    FLAG(5, true, "-FL", Message.DOORTYPE_FLAG, DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO,
         DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER, DoorAttribute.ADDOWNER,
         DoorAttribute.REMOVEOWNER),

    GARAGEDOOR(6, true, "-GD", Message.DOORTYPE_GARAGEDOOR, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
               DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.CHANGETIMER,
               DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2),

    WINDMILL(7, true, "-WM", Message.DOORTYPE_WINDMILL, DoorAttribute.LOCK, DoorAttribute.TOGGLE,
             DoorAttribute.INFO, DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER,
             DoorAttribute.REMOVEOWNER, DoorAttribute.DIRECTION_ROTATE_VERTICAL2),

    REVOLVINGDOOR(8, true, "-RD", Message.DOORTYPE_REVOLVINGDOOR, EDoorType.BIGDOOR.attributes),

    CLOCK(9, true, "-CL", Message.DOORTYPE_CLOCK, DoorAttribute.LOCK, DoorAttribute.TOGGLE, DoorAttribute.INFO,
          DoorAttribute.DELETE, DoorAttribute.RELOCATEPOWERBLOCK, DoorAttribute.ADDOWNER, DoorAttribute.REMOVEOWNER),
    ;

    private static final List<EDoorType> cachedValues = Collections.unmodifiableList(Arrays.asList(EDoorType.values()));
    private static final Map<Integer, EDoorType> valMap = new HashMap<>();
    private static final Map<String, EDoorType> commandFlagMap = new HashMap<>();

    static
    {
        for (EDoorType type : EDoorType.cachedValues())
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
    public static List<EDoorType> cachedValues()
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
     * The attributes of this door. For example, a {@link EDoorType#BIGDOOR} would have no use for {@link
     * DoorAttribute#BLOCKSTOMOVE} because it moves in quarter circles, not in numbers of blocks.
     */
    private final DoorAttribute[] attributes;

    EDoorType(final int val, final boolean enabled, final @NotNull String commandFlag, final @NotNull Message message,
              final @NotNull DoorAttribute... attributes)
    {
        this.val = val;
        this.enabled = enabled;
        this.commandFlag = commandFlag;
        this.message = message;
        this.attributes = attributes;
    }

    /**
     * Gets the index value of a {@link EDoorType}.
     *
     * @param type The {@link EDoorType}.
     * @return The index value of a {@link EDoorType}.
     */
    public static int getValue(final @NotNull EDoorType type)
    {
        return type.val;
    }

    /**
     * Gets the {@link Message} associated with the name of a {@link EDoorType}.
     *
     * @param type The {@link EDoorType}.
     * @return The {@link Message} associated with the name of a {@link EDoorType}.
     */
    @NotNull
    public static Message getMessage(final @NotNull EDoorType type)
    {
        return type.message;
    }

    /**
     * Gets the {@link EDoorType} from an index value.
     *
     * @param type The index value of this type.
     * @return The {@link EDoorType} with this index value.
     */
    @NotNull
    public static EDoorType valueOf(final int type)
        throws IllegalArgumentException
    {
        if (type >= valMap.size())
            throw new IllegalArgumentException(
                "Requested DoorType(" + type + "), but only " + valMap.size() + " are available!");
        return valMap.get(type);
    }

    /**
     * Gets the {@link EDoorType} from its commandFlag, e.g. "-BD" for {@link EDoorType#BIGDOOR}).
     *
     * @param commandFlag The command flag
     * @return The {@link EDoorType} associated with this command flag or null if none was found.
     */
    public static Optional<EDoorType> valueOfCommandFlag(final @NotNull String commandFlag)
    {
        return Optional.ofNullable(commandFlagMap.getOrDefault(commandFlag, null));
    }

    /**
     * Gets all {@link DoorAttribute}s of the {@link EDoorType}.
     *
     * @param type The {@link EDoorType}.
     * @return All {@link DoorAttribute}s of the {@link EDoorType}.
     */
    @NotNull
    public static DoorAttribute[] getAttributes(final @NotNull EDoorType type)
    {
        return type.attributes;
    }

    /**
     * Checks if a {@link EDoorType} is enabled or not.
     *
     * @param type The {@link EDoorType}.
     * @return True if a {@link EDoorType} is enabled.
     */
    public static boolean isEnabled(final @NotNull EDoorType type)
    {
        return type.enabled;
    }
}


