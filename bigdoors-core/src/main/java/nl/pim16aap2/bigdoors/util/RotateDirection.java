package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents all directions a door can rotate in.
 *
 * @author Pim
 */
public enum RotateDirection
{
    NONE(0, Message.GENERAL_DIRECTION_NONE),
    CLOCKWISE(1, Message.GENERAL_DIRECTION_CLOCKWISE),
    COUNTERCLOCKWISE(2, Message.GENERAL_DIRECTION_COUNTERCLOCKWISE),
    UP(3, Message.GENERAL_DIRECTION_UP),
    DOWN(4, Message.GENERAL_DIRECTION_DOWN),
    NORTH(5, Message.GENERAL_DIRECTION_NORTH),
    EAST(6, Message.GENERAL_DIRECTION_EAST),
    SOUTH(7, Message.GENERAL_DIRECTION_SOUTH),
    WEST(8, Message.GENERAL_DIRECTION_WEST);

    /**
     * Map of all indices with their respective {@link RotateDirection} constants as values.
     */
    private static Map<Integer, RotateDirection> map = new HashMap<>();

    static
    {
        for (RotateDirection dir : RotateDirection.values())
            map.put(dir.val, dir);
    }

    private final int val;
    private final @NotNull Message message;

    RotateDirection(final int val, final @NotNull Message message)
    {
        this.val = val;
        this.message = message;
    }

    /**
     * Gets the index value of a {@link RotateDirection}.
     *
     * @param dir The {@link RotateDirection}.
     * @return The index value of a {@link RotateDirection}.
     */
    public static int getValue(final @NotNull RotateDirection dir)
    {
        return dir.val;
    }

    /**
     * Gets the {@link RotateDirection} from an index value.
     *
     * @param dir The {@link RotateDirection}.
     * @return The {@link RotateDirection} associated with this index value.
     */
    @NotNull
    public static RotateDirection valueOf(final int dir)
    {
        return map.get(dir);
    }

    /**
     * Gets the {@link Message} associated with a {@link RotateDirection}.
     *
     * @param dir The {@link RotateDirection}.
     * @return The {@link Message} associated with a {@link RotateDirection}.
     */
    @NotNull
    public static Message getMessage(final @NotNull RotateDirection dir)
    {
        return dir.message;
    }

    /**
     * Get the {@link RotateDirection} that's the exact opposite of the provided one. For example, the opposite side of
     * {@link RotateDirection#UP} is {@link RotateDirection#DOWN}.
     *
     * @param dir The current {@link RotateDirection}
     * @return The opposite direction of the current {@link RotateDirection}.
     */
    @NotNull
    public static RotateDirection getOpposite(final @NotNull RotateDirection dir)
    {
        switch (dir)
        {
            case DOWN:
                return RotateDirection.UP;
            case EAST:
                return RotateDirection.WEST;
            case NORTH:
                return RotateDirection.SOUTH;
            case SOUTH:
                return RotateDirection.NORTH;
            case UP:
                return RotateDirection.DOWN;
            case WEST:
                return RotateDirection.EAST;
            case CLOCKWISE:
                return RotateDirection.COUNTERCLOCKWISE;
            case COUNTERCLOCKWISE:
                return RotateDirection.CLOCKWISE;
            case NONE:
            default:
                return RotateDirection.NONE;
        }
    }
}
