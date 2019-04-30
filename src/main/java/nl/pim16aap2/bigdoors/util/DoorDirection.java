package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorDirection
{
    NORTH (0),
    EAST  (1),
    SOUTH (2),
    WEST  (3);

    private int val;
    private static Map<Integer, DoorDirection> map = new HashMap<>();

    private DoorDirection(int val)
    {
        this.val = val;
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
