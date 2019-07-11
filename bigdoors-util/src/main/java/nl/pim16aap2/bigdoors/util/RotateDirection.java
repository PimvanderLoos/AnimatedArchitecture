package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents all directions a door can rotate in.
 *
 * @author Pim
 */
public enum RotateDirection
{
    NONE(0, "GUI.Direction.Any"),
    CLOCKWISE(1, "GUI.Direction.Clock"),
    COUNTERCLOCKWISE(2, "GUI.Direction.Counter"),
    UP(3, "GUI.Direction.Up"),
    DOWN(4, "GUI.Direction.Down"),
    NORTH(5, "GUI.Direction.North"),
    EAST(6, "GUI.Direction.East"),
    SOUTH(7, "GUI.Direction.South"),
    WEST(8, "GUI.Direction.West");

    private static Map<Integer, RotateDirection> map = new HashMap<>();

    static
    {
        for (RotateDirection dir : RotateDirection.values())
            map.put(dir.val, dir);
    }

    private int val;
    private String nameKey;

    RotateDirection(int val, String nameKey)
    {
        this.val = val;
        this.nameKey = nameKey;
    }

    public static int getValue(RotateDirection dir)
    {
        return dir.val;
    }

    public static RotateDirection valueOf(int dir)
    {
        return map.get(dir);
    }

    public static String getNameKey(RotateDirection dir)
    {
        return dir.nameKey;
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
