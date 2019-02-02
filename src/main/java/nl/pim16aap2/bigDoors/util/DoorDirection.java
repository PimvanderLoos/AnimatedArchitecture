package nl.pim16aap2.bigDoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorDirection
{
    // Direction // Offset angle // Val
    NORTH        (1.0 * Math.PI, 0),
    EAST         (0.5 * Math.PI, 1),
    SOUTH        (0.0 * Math.PI, 2),
    WEST         (1.5 * Math.PI, 3);

    private double extraAngle;
    private int val;
    private static Map<Integer, DoorDirection> map = new HashMap<Integer, DoorDirection>();

    private DoorDirection(double extraAngle, int val)
    {
        this.extraAngle = extraAngle;
        this.val = val;
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
}
