package nl.pim16aap2.bigDoors.util;

import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum MyBlockFace
{
    NORTH(0, new Vector3D(0, 0, -1), BlockFace.NORTH),
    EAST(1, new Vector3D(1, 0, 0), BlockFace.EAST),
    SOUTH(2, new Vector3D(0, 0, 1), BlockFace.SOUTH),
    WEST(3, new Vector3D(-1, 0, 0), BlockFace.WEST),
    UP(4, new Vector3D(0, 1, 0), BlockFace.UP),
    DOWN(5, new Vector3D(0, -1, 0), BlockFace.DOWN);

    private final BlockFace bukkitFace;
    private final Vector3D dir;
    private final int val;
    private static Map<BlockFace, MyBlockFace> map = new HashMap<>();
    private static Map<Vector3D, MyBlockFace> dirs = new HashMap<>();
    private static Map<Integer, MyBlockFace> vals = new HashMap<>();
    private static final List<MyBlockFace> VALUES = Collections.unmodifiableList(Arrays.asList(MyBlockFace.values()));

    MyBlockFace(final int val, final Vector3D dir, final BlockFace bukkitFace)
    {
        this.val = val;
        this.bukkitFace = bukkitFace;
        this.dir = dir;
    }

    public static int getValue(MyBlockFace dir)
    {
        return dir.val;
    }

    public static MyBlockFace valueOf(int val)
    {
        return vals.get(val);
    }

    public static BlockFace getBukkitFace(MyBlockFace myFace)
    {
        return myFace.bukkitFace;
    }

    public static MyBlockFace getMyBlockFace(BlockFace bukkitFace)
    {
        return map.get(bukkitFace);
    }

    public static Vector3D getDirection(MyBlockFace myFace)
    {
        return myFace.dir;
    }

    /*  Rotate horizontally in clockwise  direction.
     */
    public static MyBlockFace rotateClockwise(MyBlockFace myFace)
    {
        switch (myFace)
        {
        case NORTH:
            return EAST;
        case EAST:
            return SOUTH;
        case SOUTH:
            return WEST;
        case WEST:
            return NORTH;
        default:
            return myFace;
        }
    }

    /*  Rotate horizontally in counter clockwise  direction.
     */
    public static MyBlockFace rotateCounterClockwise(MyBlockFace myFace)
    {
        switch (myFace)
        {
        case NORTH:
            return WEST;
        case EAST:
            return NORTH;
        case SOUTH:
            return EAST;
        case WEST:
            return SOUTH;
        default:
            return myFace;
        }
    }

    /*
     * Rotate a direction vertically along the North/South axis (Z-axis).
     * In northern direction as seen from the up position.
     */
    public static MyBlockFace rotateVerticallyNorth(MyBlockFace curFace)
    {
        switch(curFace)
        {
        case EAST:
        case WEST:
            return curFace;
        case DOWN:
            return MyBlockFace.SOUTH;
        case NORTH:
            return MyBlockFace.DOWN;
        case SOUTH:
            return MyBlockFace.UP;
        case UP:
            return MyBlockFace.NORTH;
        default:
            return null;
        }
    }

    /*
     * Rotate a direction vertically along the North/South axis (Z-axis).
     * In southern direction as seen from the up position.
     */
    public static MyBlockFace rotateVerticallySouth(MyBlockFace curFace)
    {
        switch(curFace)
        {
        case EAST:
        case WEST:
            return curFace;
        case DOWN:
            return MyBlockFace.NORTH;
        case NORTH:
            return MyBlockFace.UP;
        case SOUTH:
            return MyBlockFace.DOWN;
        case UP:
            return MyBlockFace.SOUTH;
        default:
            return null;
        }
    }

    /*
     * Rotate a direction vertically along the East/West axis (X-axis).
     * In eastern direction as seen from the up position.
     */
    public static MyBlockFace rotateVerticallyEast(MyBlockFace curFace)
    {
        switch(curFace)
        {
        case NORTH:
        case SOUTH:
            return curFace;
        case DOWN:
            return MyBlockFace.WEST;
        case EAST:
            return MyBlockFace.DOWN;
        case WEST:
            return MyBlockFace.UP;
        case UP:
            return MyBlockFace.EAST;
        default:
            return null;
        }
    }

    /*
     * Rotate a direction vertically along the East/West axis (X-axis).
     * In western direction as seen from the up position.
     */
    public static MyBlockFace rotateVerticallyWest(MyBlockFace curFace)
    {
        switch(curFace)
        {
        case NORTH:
        case SOUTH:
            return curFace;
        case DOWN:
            return MyBlockFace.EAST;
        case EAST:
            return MyBlockFace.UP;
        case WEST:
            return MyBlockFace.DOWN;
        case UP:
            return MyBlockFace.WEST;
        default:
            return null;
        }
    }

    public static MyBlockFace faceFromDir(Vector3D dir)
    {
        return dirs.get(dir);
    }

    static
    {
        for (MyBlockFace face : MyBlockFace.values())
        {
            map.put(face.bukkitFace, face);
            dirs.put(face.dir, face);
            vals.put(face.val, face);
        }
    }

    public static List<MyBlockFace> getValues()
    {
        return VALUES;
    }
}
