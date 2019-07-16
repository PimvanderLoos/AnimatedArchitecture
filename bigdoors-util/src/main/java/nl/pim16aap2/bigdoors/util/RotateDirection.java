package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.messages.Message;

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

    private static Map<Integer, RotateDirection> map = new HashMap<>();

    static
    {
        for (RotateDirection dir : RotateDirection.values())
            map.put(dir.val, dir);
    }

    private int val;
    private Message message;

    RotateDirection(int val, Message message)
    {
        this.val = val;
        this.message = message;
    }

    public static int getValue(RotateDirection dir)
    {
        return dir.val;
    }

    public static RotateDirection valueOf(int dir)
    {
        return map.get(dir);
    }

    public static Message getMessage(RotateDirection dir)
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
    public static RotateDirection getOpposite(RotateDirection dir)
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
                return null;
        }
    }
}
