package nl.pim16aap2.bigDoors.util;

import java.util.HashMap;
import java.util.Map;

public enum RotateDirection
{
    NONE             (0),
    CLOCKWISE        (1),
    COUNTERCLOCKWISE (2),
    UP               (3),
    DOWN             (4);

    private int val;
    private static Map<Integer, RotateDirection> map = new HashMap<Integer, RotateDirection>();

    private RotateDirection(int val)
    {
        this.val = val;
    }

    public static int getValue(RotateDirection dir)
    {
        return dir.val;
    }

    public static RotateDirection valueOf(int dir)
    {
        return map.get(dir);
    }

    static
    {
        for (RotateDirection dir : RotateDirection.values())
            map.put(dir.val, dir);
    }
}
