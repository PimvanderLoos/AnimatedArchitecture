package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Represents the 6 faces of a block.
 *
 * @author Pim
 */
public enum PBlockFace
{
    NORTH(0, new Vector3Di(0, 0, -1)),
    EAST(1, new Vector3Di(1, 0, 0)),
    SOUTH(2, new Vector3Di(0, 0, 1)),
    WEST(3, new Vector3Di(-1, 0, 0)),
    UP(4, new Vector3Di(0, 1, 0)),
    DOWN(5, new Vector3Di(0, -1, 0)),
    NONE(6, new Vector3Di(0, 0, 0)),
    ;
    private static final Map<Vector3Di, PBlockFace> DIRS = new HashMap<>();
    private static final Map<Integer, PBlockFace> VALS = new HashMap<>();

    static
    {
        for (PBlockFace face : PBlockFace.values())
        {
            DIRS.put(face.directionVector, face);
            VALS.put(face.val, face);
        }
    }

    /**
     * The vector of this {@link PBlockFace}. For example, {@link PBlockFace#UP} would be (0,1,0), as it's direction is
     * positive in the y-axis and 0 in every other direction.
     */
    private final Vector3Di directionVector;
    private final int val;

    PBlockFace(int val, Vector3Di directionVector)
    {
        this.val = val;
        this.directionVector = directionVector;
    }

    /**
     * Get the {@link PBlockFace} that's the exact opposite of the provided one. For example, the opposite side of
     * {@link PBlockFace#UP} is {@link PBlockFace#DOWN}.
     *
     * @param dir The current {@link PBlockFace}
     * @return The opposite direction of the current {@link PBlockFace}.
     */
    @SuppressWarnings("unused")
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
            case NONE:
            default:
                return PBlockFace.NONE;
        }
    }

    /**
     * Gets the integer value of a {@link PBlockFace}.
     *
     * @param dir The {@link PBlockFace}.
     * @return The integer value of a {@link PBlockFace}.
     */
    public static int getValue(PBlockFace dir)
    {
        return dir.val;
    }

    // TODO: Optional
    @SuppressWarnings("unused")
    public static @Nullable PBlockFace valueOf(int val)
    {
        return VALS.get(val);
    }

    /**
     * Get the {@link PBlockFace#directionVector} of this {@link PBlockFace}
     *
     * @param myFace The direction.
     * @return The vector of the direction.
     */
    public static Vector3Di getDirection(PBlockFace myFace)
    {
        return myFace.directionVector;
    }

    /**
     * Rotate the {@link PBlockFace} in clockwise direction from a perspective of looking down on the world. For
     * example, {@link PBlockFace#NORTH} would return {@link PBlockFace#EAST}.
     *
     * @param myFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    @SuppressWarnings("DuplicatedCode") // It's actually different from rotating counterclockwise...
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
     * Rotate the {@link PBlockFace} in counter clockwise direction from a perspective of looking down on the world. For
     * example, {@link PBlockFace#NORTH} would return {@link PBlockFace#WEST}.
     *
     * @param myFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    @SuppressWarnings("DuplicatedCode") // It's actually different from rotating clockwise...
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
     * Rotate the {@link PBlockFace} in northern direction on the vertical plane. For example, {@link PBlockFace#UP}
     * would return {@link PBlockFace#NORTH} and {@link PBlockFace#NORTH} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallyNorth(PBlockFace curFace)
    {
        switch (curFace)
        {
            case DOWN:
                return PBlockFace.SOUTH;
            case NORTH:
                return PBlockFace.DOWN;
            case SOUTH:
                return PBlockFace.UP;
            case UP:
                return PBlockFace.NORTH;
            case EAST, WEST:
            default:
                return curFace;
        }
    }

    /**
     * Rotate the {@link PBlockFace} in southern direction on the vertical plane. For example, {@link PBlockFace#UP}
     * would return {@link PBlockFace#SOUTH} and {@link PBlockFace#SOUTH} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallySouth(PBlockFace curFace)
    {
        switch (curFace)
        {
            case DOWN:
                return PBlockFace.NORTH;
            case NORTH:
                return PBlockFace.UP;
            case SOUTH:
                return PBlockFace.DOWN;
            case UP:
                return PBlockFace.SOUTH;
            case EAST, WEST:
            default:
                return curFace;
        }
    }

    /**
     * Rotate the {@link PBlockFace} in eastern direction on the vertical plane. For example, {@link PBlockFace#UP}
     * would return {@link PBlockFace#EAST} and {@link PBlockFace#EAST} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallyEast(PBlockFace curFace)
    {
        switch (curFace)
        {
            case DOWN:
                return PBlockFace.WEST;
            case EAST:
                return PBlockFace.DOWN;
            case WEST:
                return PBlockFace.UP;
            case UP:
                return PBlockFace.EAST;
            case NORTH, SOUTH:
            default:
                return curFace;
        }
    }

    /**
     * Rotate the {@link PBlockFace} in western direction on the vertical plane. For example, {@link PBlockFace#UP}
     * would return {@link PBlockFace#WEST} and {@link PBlockFace#WEST} would return {@link PBlockFace#DOWN}.
     *
     * @param curFace The current {@link PBlockFace}.
     * @return The rotated {@link PBlockFace}.
     */
    public static PBlockFace rotateVerticallyWest(PBlockFace curFace)
    {
        switch (curFace)
        {
            case DOWN:
                return PBlockFace.EAST;
            case EAST:
                return PBlockFace.UP;
            case WEST:
                return PBlockFace.DOWN;
            case UP:
                return PBlockFace.WEST;
            case NORTH, SOUTH:
            default:
                return curFace;
        }
    }

    /**
     * Get the blockFace from a {@link PBlockFace#directionVector} value.
     *
     * @param dir The {@link PBlockFace#directionVector}.
     * @return The {@link PBlockFace} associated with this {@link PBlockFace#directionVector}.
     */
    @SuppressWarnings("unused")
    public static Optional<PBlockFace> faceFromDir(Vector3Di dir)
    {
        return Optional.ofNullable(DIRS.get(dir));
    }

    /**
     * Get the appropriate function for rotating a BlockFace. Different rotation directions use different methods.
     *
     * @param rotDir The {@link RotateDirection} to rotate in.
     * @return The appropriate function for rotating the {@link PBlockFace} in the given direction.
     */
    // TODO: OPTIONAL
    public static @Nullable UnaryOperator<PBlockFace> getDirFun(RotateDirection rotDir)
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
            case DOWN, UP, NONE:
            default:
                return null;
        }
    }

    /**
     * Rotate a PBlockFace in a given direction for a number of steps.
     *
     * @param pbf   The {@link PBlockFace} that will be rotated.
     * @param steps The number of times to apply the rotation.
     * @param dir   The function the applies the rotation.
     * @return The rotated {@link PBlockFace}.
     *
     * @see PBlockFace#getDirFun
     */
    public static PBlockFace rotate(PBlockFace pbf, int steps, UnaryOperator<PBlockFace> dir)
    {
        if (pbf.equals(PBlockFace.NONE))
            return pbf;
        // Every 4 steps results in the same outcome.
        steps = steps % 4;
        if (steps == 0)
            return pbf;

        PBlockFace newFace = pbf;
        while (steps-- > 0)
            newFace = dir.apply(pbf);
        return newFace;
    }
}
