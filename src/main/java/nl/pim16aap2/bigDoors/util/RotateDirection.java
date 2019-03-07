package nl.pim16aap2.bigDoors.util;

import java.util.HashMap;
import java.util.Map;

public enum RotateDirection
{
    NONE             (0, "GUI.Direction.Any"),
    CLOCKWISE        (1, "GUI.Direction.Clock"),
    COUNTERCLOCKWISE (2, "GUI.Direction.Counter"),
    UP               (3, "GUI.Direction.Up"),
    DOWN             (4, "GUI.Direction.Down"),
    NORTH            (5, "GUI.Direction.North"),
    EAST             (6, "GUI.Direction.East"),
    SOUTH            (7, "GUI.Direction.South"),
    WEST             (8, "GUI.Direction.West");

    private int val;
    private String nameKey;
    private static Map<Integer, RotateDirection> map = new HashMap<Integer, RotateDirection>();

    private RotateDirection(int val, String nameKey)
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

    static
    {
        for (RotateDirection dir : RotateDirection.values())
            map.put(dir.val, dir);
    }
}
