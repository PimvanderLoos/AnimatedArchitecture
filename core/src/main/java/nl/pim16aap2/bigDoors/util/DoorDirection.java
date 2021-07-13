package nl.pim16aap2.bigDoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorDirection
{
    // Direction // Offset angle // Val
    NORTH(1.0 * Math.PI, 0),
    EAST(0.5 * Math.PI, 1),
    SOUTH(0.0 * Math.PI, 2),
    WEST(1.5 * Math.PI, 3);

    private double extraAngle;
    private int val;
    private static Map<Integer, DoorDirection> map = new HashMap<>();
    private static final DoorDirection[] DOOR_DIRECTIONS = DoorDirection.values();

    private DoorDirection(double extraAngle, int val)
    {
        this.extraAngle = extraAngle;
        this.val = val;
    }

    public static DoorDirection getOpposite(DoorDirection dir)
    {
        switch (dir)
        {
            case NORTH:
                return SOUTH;
            case EAST:
                return WEST;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
        }
        return null;
    }

    public static double getExtraAngle(DoorDirection dir)
    {
        return dir.extraAngle;
    }

    public static int getValue(DoorDirection dir)
    {
        return dir.val;
    }

    public static DoorDirection valueOf(int dir)
    {
        return map.get(dir);
    }

    static
    {
        for (DoorDirection dir : DoorDirection.values())
            map.put(dir.val, dir);
    }

    private static DoorDirection cycleCardinalDirection(int steps, DoorDirection dir)
    {
        final int curIdx = dir.ordinal() + DOOR_DIRECTIONS.length;
        return DOOR_DIRECTIONS[(curIdx + steps) % DOOR_DIRECTIONS.length];
    }

    public static DoorDirection cycleCardinalDirection(DoorDirection dir)
    {
        return cycleCardinalDirection(1, dir);
    }

    public static DoorDirection cycleCardinalDirectionReverse(DoorDirection dir)
    {
        return cycleCardinalDirection(-1, dir);
    }
}
