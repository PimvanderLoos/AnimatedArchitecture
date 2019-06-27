package nl.pim16aap2.bigdoors.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents the 6 faces of a block.
 *
 * @author Pim
 */
public enum PBlockFace
{
    NORTH (0, new Vector3D(0, 0, -1)),
    EAST (1, new Vector3D(1, 0, 0)),
    SOUTH (2, new Vector3D(0, 0, 1)),
    WEST (3, new Vector3D(-1, 0, 0)),
    UP (4, new Vector3D(0, 1, 0)),
    DOWN (5, new Vector3D(0, -1, 0));

    /**
     * The vector of this {@link PBlockFace}. For example, {@link PBlockFace#UP}
     * would be (0,1,0), as it's direction is positive in the y-axis and 0 in every
     * other direction.
     */
    private final Vector3D directionVector;
    private final int val;
    private static Map<Vector3D, PBlockFace> dirs = new HashMap<>();
    private static Map<Integer, PBlockFace> vals = new HashMap<>();

    PBlockFace(final int val, final Vector3D directionVector)
    {
        this.val = val;
        this.directionVector = directionVector;
    }

    /**
     * Get the {@link PBlockFace} that's the exact opposite of the provided one.
     * For example, the opposite side of {@link PBlockFace#UP} is
     * {@link PBlockFace#DOWN}.
     *
     * @param dir The current {@link PBlockFace}
     * @return The opposite direction of the current {@link PBlockFace}.
     */
    public static PBlockFace getOpposite(PBlockFace dir)
    {
        switch (dir)
        {
        case DOWN:
            return PBlockFace.UP;
        case EAST:
            return PBlockFace.WEST;
        case NORTH:
            return PBlockFace.SOUTH;
        case SOUTH:
            return PBlockFace.NORTH;
        case UP:
            return PBlockFace.DOWN;
        case WEST:
            return PBlockFace.EAST;
        default:
            return null;
        }
    }

    public static int getValue(PBlockFace dir)
    {
        return dir.val;
    }

    public static PBlockFace valueOf(int val)
    {
        return vals.get(val);
    }

    /**
     * Get the {@link PBlockFace#directionVector} of this {@link PBlockFace}
     * 
     * @param myFace The direction.
     * @return The vector of the direction.
     */
    public static Vector3D getDirection(PBlockFace myFace)
    {
        return myFace.directionVector;
    }

    /**
     * Rotate the {@link PBlockFace} in clockwise direction from a perspective of
     * looking down on the world. For example, {@link PBlockFace#NORTH} would
     * return {@link PBlockFace#EAST}.
     * 
     * @param myFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateClockwise(PBlockFace myFace)
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
     * Rotate the {@link PBlockFace} in counter clockwise direction from a
     * perspective of looking down on the world. For example,
     * {@link PBlockFace#NORTH} would return {@link PBlockFace#WEST}.
     *
     * @param myFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateCounterClockwise(PBlockFace myFace)
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
     * Rotate the {@link PBlockFace} in northern direction on the vertical plane.
     * For example, {@link PBlockFace#UP} would return {@link PBlockFace#NORTH}
     * and {@link PBlockFace#NORTH} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallyNorth(PBlockFace curFace)
    {
        switch (curFace)
        {
        case EAST:
        case WEST:
            return curFace;
        case DOWN:
            return PBlockFace.SOUTH;
        case NORTH:
            return PBlockFace.DOWN;
        case SOUTH:
            return PBlockFace.UP;
        case UP:
            return PBlockFace.NORTH;
        default:
            return null;
        }
    }

    /**
     * Rotate the {@link PBlockFace} in southern direction on the vertical plane.
     * For example, {@link PBlockFace#UP} would return {@link PBlockFace#SOUTH}
     * and {@link PBlockFace#SOUTH} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallySouth(PBlockFace curFace)
    {
        switch (curFace)
        {
        case EAST:
        case WEST:
            return curFace;
        case DOWN:
            return PBlockFace.NORTH;
        case NORTH:
            return PBlockFace.UP;
        case SOUTH:
            return PBlockFace.DOWN;
        case UP:
            return PBlockFace.SOUTH;
        default:
            return null;
        }
    }

    /**
     * Rotate the {@link PBlockFace} in eastern direction on the vertical plane.
     * For example, {@link PBlockFace#UP} would return {@link PBlockFace#EAST} and
     * {@link PBlockFace#EAST} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallyEast(PBlockFace curFace)
    {
        switch (curFace)
        {
        case NORTH:
        case SOUTH:
            return curFace;
        case DOWN:
            return PBlockFace.WEST;
        case EAST:
            return PBlockFace.DOWN;
        case WEST:
            return PBlockFace.UP;
        case UP:
            return PBlockFace.EAST;
        default:
            return null;
        }
    }

    /**
     * Rotate the {@link PBlockFace} in western direction on the vertical plane.
     * For example, {@link PBlockFace#UP} would return {@link PBlockFace#WEST} and
     * {@link PBlockFace#WEST} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallyWest(PBlockFace curFace)
    {
        switch (curFace)
        {
        case NORTH:
        case SOUTH:
            return curFace;
        case DOWN:
            return PBlockFace.EAST;
        case EAST:
            return PBlockFace.UP;
        case WEST:
            return PBlockFace.DOWN;
        case UP:
            return PBlockFace.WEST;
        default:
            return null;
        }
    }

    /**
     * Get the blockFace from a {@link PBlockFace#directionVector} value.
     *
     * @param dir The {@link PBlockFace#directionVector}.
     * @return The {@link PBlockFace} associated with this
     *         {@link PBlockFace#directionVector}.
     */
    public static PBlockFace faceFromDir(Vector3D dir)
    {
        return dirs.get(dir);
    }

    static
    {
        for (PBlockFace face : PBlockFace.values())
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
     * @return The appropriate function for rotating the {@link PBlockFace} in the
     *         given direction.
     */
    public static Function<PBlockFace, PBlockFace> getDirFun(RotateDirection rotDir)
    {
        switch (rotDir)
        {
        case NORTH:
            return PBlockFace::rotateVerticallyNorth;
        case EAST:
            return PBlockFace::rotateVerticallyEast;
        case SOUTH:
            return PBlockFace::rotateVerticallySouth;
        case WEST:
            return PBlockFace::rotateVerticallyWest;
        case CLOCKWISE:
            return PBlockFace::rotateClockwise;
        case COUNTERCLOCKWISE:
            return PBlockFace::rotateCounterClockwise;
        case DOWN:
        case UP:
        case NONE:
        default:
            return null;
        }
    }

    /**
     * Rotate a PBlockFace in a given direction for a number of steps.
     * 
     * @param mbf   The {@link PBlockFace} that will be rotated.
     * @param steps The number of times to apply the rotation.
     * @param dir   The function the applies the rotation.
     * @return The rotated {@link PBlockFace}.
     * @see PBlockFace#getDirFun
     */
    public static PBlockFace rotate(PBlockFace mbf, int steps, Function<PBlockFace, PBlockFace> dir)
    {
        // Every 4 steps results in the same outcome.
        steps = steps % 4;
        if (steps == 0)
            return mbf;

        PBlockFace newFace = mbf;
        while (steps-- > 0)
            newFace = dir.apply(mbf);
        return newFace;
    }
}
