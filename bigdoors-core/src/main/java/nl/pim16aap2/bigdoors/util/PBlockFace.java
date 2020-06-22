package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    NORTH(0, new Vector3Di(0, 0, -1)),
    EAST(1, new Vector3Di(1, 0, 0)),
    SOUTH(2, new Vector3Di(0, 0, 1)),
    WEST(3, new Vector3Di(-1, 0, 0)),
    UP(4, new Vector3Di(0, 1, 0)),
    DOWN(5, new Vector3Di(0, -1, 0)),
    NONE(6, new Vector3Di(0, 0, 0)),
    ;

    private static Map<IVector3DiConst, PBlockFace> dirs = new HashMap<>();
    private static Map<Integer, PBlockFace> vals = new HashMap<>();

    static
    {
        for (PBlockFace face : PBlockFace.values())
        {
            dirs.put(face.directionVector, face);
            vals.put(face.val, face);
        }
    }

    /**
     * The vector of this {@link PBlockFace}. For example, {@link PBlockFace#UP} would be (0,1,0), as it's direction is
     * positive in the y-axis and 0 in every other direction.
     */
    private final IVector3DiConst directionVector;
    private final int val;

    PBlockFace(final int val, final @NotNull IVector3DiConst directionVector)
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
    @NotNull
    public static PBlockFace getOpposite(final @NotNull PBlockFace dir)
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
    public static int getValue(final @NotNull PBlockFace dir)
    {
        return dir.val;
    }

    @Nullable // TODO: Optional
    public static PBlockFace valueOf(final int val)
    {
        return vals.get(val);
    }

    /**
     * Get the {@link PBlockFace#directionVector} of this {@link PBlockFace}
     *
     * @param myFace The direction.
     * @return The vector of the direction.
     */
    @NotNull
    public static IVector3DiConst getDirection(final @NotNull PBlockFace myFace)
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
    @NotNull
    public static PBlockFace rotateClockwise(final @NotNull PBlockFace myFace)
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
    @NotNull
    public static PBlockFace rotateCounterClockwise(final @NotNull PBlockFace myFace)
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
    @NotNull
    public static PBlockFace rotateVerticallyNorth(final @NotNull PBlockFace curFace)
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
            case EAST:
            case WEST:
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
    @NotNull
    public static PBlockFace rotateVerticallySouth(final @NotNull PBlockFace curFace)
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
            case EAST:
            case WEST:
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
    @NotNull
    public static PBlockFace rotateVerticallyEast(final @NotNull PBlockFace curFace)
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
            case NORTH:
            case SOUTH:
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
    @NotNull
    public static PBlockFace rotateVerticallyWest(final @NotNull PBlockFace curFace)
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
            case NORTH:
            case SOUTH:
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
    @NotNull
    public static PBlockFace faceFromDir(final @NotNull IVector3DiConst dir)
    {
        return dirs.get(dir);
    }

    /**
     * Get the appropriate function for rotating a BlockFace. Different rotation directions use different methods.
     *
     * @param rotDir The {@link RotateDirection} to rotate in.
     * @return The appropriate function for rotating the {@link PBlockFace} in the given direction.
     */
    @Nullable // TODO: OPTIONAL
    public static Function<PBlockFace, PBlockFace> getDirFun(final @NotNull RotateDirection rotDir)
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
     * @param pbf   The {@link PBlockFace} that will be rotated.
     * @param steps The number of times to apply the rotation.
     * @param dir   The function the applies the rotation.
     * @return The rotated {@link PBlockFace}.
     *
     * @see PBlockFace#getDirFun
     */
    @NotNull
    public static PBlockFace rotate(final @NotNull PBlockFace pbf, int steps,
                                    final @NotNull Function<PBlockFace, PBlockFace> dir)
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
