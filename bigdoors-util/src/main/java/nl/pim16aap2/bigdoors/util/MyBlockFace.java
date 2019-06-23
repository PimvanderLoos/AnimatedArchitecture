package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents the 6 faces of a block.
 *
 * @author Pim
 */
public enum MyBlockFace
{
    NORTH (0, new Vector3D(0, 0, -1)),
    EAST (1, new Vector3D(1, 0, 0)),
    SOUTH (2, new Vector3D(0, 0, 1)),
    WEST (3, new Vector3D(-1, 0, 0)),
    UP (4, new Vector3D(0, 1, 0)),
    DOWN (5, new Vector3D(0, -1, 0));

    /**
     * The vector of this {@link MyBlockFace}. For example, {@link MyBlockFace#UP}
     * would be (0,1,0), as it's direction is positive in the y-axis and 0 in every
     * other direction.
     */
    private final Vector3D directionVector;
    private final int val;
    private static Map<Vector3D, MyBlockFace> dirs = new HashMap<>();
    private static Map<Integer, MyBlockFace> vals = new HashMap<>();

    MyBlockFace(final int val, final Vector3D directionVector)
    {
        this.val = val;
        this.directionVector = directionVector;
    }

    /**
     * Get the {@link MyBlockFace} that's the exact opposite of the provided one.
     * For example, the opposite side of {@link MyBlockFace#UP} is
     * {@link MyBlockFace#DOWN}.
     *
     * @param dir The current {@link MyBlockFace}
     * @return The opposite direction of the current {@link MyBlockFace}.
     */
    public static MyBlockFace getOpposite(MyBlockFace dir)
    {
        switch (dir)
        {
        case DOWN:
            return MyBlockFace.UP;
        case EAST:
            return MyBlockFace.WEST;
        case NORTH:
            return MyBlockFace.SOUTH;
        case SOUTH:
            return MyBlockFace.NORTH;
        case UP:
            return MyBlockFace.DOWN;
        case WEST:
            return MyBlockFace.EAST;
        default:
            return null;
        }
    }

    public static int getValue(MyBlockFace dir)
    {
        return dir.val;
    }

    public static MyBlockFace valueOf(int val)
    {
        return vals.get(val);
    }

    /**
     * Get the {@link MyBlockFace#directionVector} of this {@link MyBlockFace}
     * 
     * @param myFace The direction.
     * @return The vector of the direction.
     */
    public static Vector3D getDirection(MyBlockFace myFace)
    {
        return myFace.directionVector;
    }

    /**
     * Rotate the {@link MyBlockFace} in clockwise direction from a perspective of
     * looking down on the world. For example, {@link MyBlockFace#NORTH} would
     * return {@link MyBlockFace#EAST}.
     * 
     * @param myFace The current {@link MyBlockFace}.
     * @return The rotated {@link MyBlockFace}.
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

    /**
     * Rotate the {@link MyBlockFace} in counter clockwise direction from a
     * perspective of looking down on the world. For example,
     * {@link MyBlockFace#NORTH} would return {@link MyBlockFace#WEST}.
     *
     * @param myFace The current {@link MyBlockFace}.
     * @return The rotated {@link MyBlockFace}.
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

    /**
     * Rotate the {@link MyBlockFace} in northern direction on the vertical plane.
     * For example, {@link MyBlockFace#UP} would return {@link MyBlockFace#NORTH}
     * and {@link MyBlockFace#NORTH} would return {@link MyBlockFace#DOWN}.
     *
     * @param curFace The current {@link MyBlockFace}.
     * @return The rotated {@link MyBlockFace}.
     */
    public static MyBlockFace rotateVerticallyNorth(MyBlockFace curFace)
    {
        switch (curFace)
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

    /**
     * Rotate the {@link MyBlockFace} in southern direction on the vertical plane.
     * For example, {@link MyBlockFace#UP} would return {@link MyBlockFace#SOUTH}
     * and {@link MyBlockFace#SOUTH} would return {@link MyBlockFace#DOWN}.
     *
     * @param curFace The current {@link MyBlockFace}.
     * @return The rotated {@link MyBlockFace}.
     */
    public static MyBlockFace rotateVerticallySouth(MyBlockFace curFace)
    {
        switch (curFace)
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

    /**
     * Rotate the {@link MyBlockFace} in eastern direction on the vertical plane.
     * For example, {@link MyBlockFace#UP} would return {@link MyBlockFace#EAST} and
     * {@link MyBlockFace#EAST} would return {@link MyBlockFace#DOWN}.
     *
     * @param curFace The current {@link MyBlockFace}.
     * @return The rotated {@link MyBlockFace}.
     */
    public static MyBlockFace rotateVerticallyEast(MyBlockFace curFace)
    {
        switch (curFace)
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

    /**
     * Rotate the {@link MyBlockFace} in western direction on the vertical plane.
     * For example, {@link MyBlockFace#UP} would return {@link MyBlockFace#WEST} and
     * {@link MyBlockFace#WEST} would return {@link MyBlockFace#DOWN}.
     *
     * @param curFace The current {@link MyBlockFace}.
     * @return The rotated {@link MyBlockFace}.
     */
    public static MyBlockFace rotateVerticallyWest(MyBlockFace curFace)
    {
        switch (curFace)
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

    /**
     * Get the blockFace from a {@link MyBlockFace#directionVector} value.
     *
     * @param dir The {@link MyBlockFace#directionVector}.
     * @return The {@link MyBlockFace} associated with this
     *         {@link MyBlockFace#directionVector}.
     */
    public static MyBlockFace faceFromDir(Vector3D dir)
    {
        return dirs.get(dir);
    }

    static
    {
        for (MyBlockFace face : MyBlockFace.values())
        {
            dirs.put(face.directionVector, face);
            vals.put(face.val, face);
        }
    }

    /**
     * Get the appropriate function for rotating a BlockFace. Different rotation
     * directions use different methods.
     * 
     * @param rotDir The {@link RotateDirection} to rotate in.
     * @return The appropriate function for rotating the {@link MyBlockFace} in the
     *         given direction.
     */
    public static Function<MyBlockFace, MyBlockFace> getDirFun(RotateDirection rotDir)
    {
        switch (rotDir)
        {
        case NORTH:
            return MyBlockFace::rotateVerticallyNorth;
        case EAST:
            return MyBlockFace::rotateVerticallyEast;
        case SOUTH:
            return MyBlockFace::rotateVerticallySouth;
        case WEST:
            return MyBlockFace::rotateVerticallyWest;
        case CLOCKWISE:
            return MyBlockFace::rotateClockwise;
        case COUNTERCLOCKWISE:
            return MyBlockFace::rotateCounterClockwise;
        case DOWN:
        case UP:
        case NONE:
        default:
            return null;
        }
    }

    /**
     * Rotate a MyBlockFace in a given direction for a number of steps.
     * 
     * @param mbf   The {@link MyBlockFace} that will be rotated.
     * @param steps The number of times to apply the rotation.
     * @param dir   The function the applies the rotation.
     * @return The rotated {@link MyBlockFace}.
     * @see MyBlockFace#getDirFun
     */
    public static MyBlockFace rotate(MyBlockFace mbf, int steps, Function<MyBlockFace, MyBlockFace> dir)
    {
        // Every 4 steps results in the same outcome.
        steps = steps % 4;
        if (steps == 0)
            return mbf;

        MyBlockFace newFace = mbf;
        while (steps-- > 0)
            newFace = dir.apply(mbf);
        return newFace;
    }
}
