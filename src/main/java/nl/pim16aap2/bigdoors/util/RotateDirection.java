package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;

public enum RotateDirection
{
    NORTH            (0, "GUI.Direction.North"),
    EAST             (1, "GUI.Direction.East"),
    SOUTH            (2, "GUI.Direction.South"),
    WEST             (3, "GUI.Direction.West"),
    UP               (4, "GUI.Direction.Up"),
    DOWN             (5, "GUI.Direction.Down"),
    NONE             (6, "GUI.Direction.Any"),
    CLOCKWISE        (7, "GUI.Direction.Clock"),
    COUNTERCLOCKWISE (8, "GUI.Direction.Counter");

    private int val;
    private String nameKey;
    private static Map<Integer, RotateDirection> map = new HashMap<>();

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
