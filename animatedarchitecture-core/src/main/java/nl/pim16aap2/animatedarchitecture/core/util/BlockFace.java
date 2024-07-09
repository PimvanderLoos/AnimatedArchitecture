package nl.pim16aap2.animatedarchitecture.core.util;

import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Represents the 6 faces of a cube.
 */
public enum BlockFace
{
    NORTH(0, new Vector3Di(0, 0, -1)),
    EAST(1, new Vector3Di(1, 0, 0)),
    SOUTH(2, new Vector3Di(0, 0, 1)),
    WEST(3, new Vector3Di(-1, 0, 0)),
    UP(4, new Vector3Di(0, 1, 0)),
    DOWN(5, new Vector3Di(0, -1, 0)),
    NONE(6, new Vector3Di(0, 0, 0)),
    ;
    private static final Map<Vector3Di, BlockFace> FROM_DIRECTION = new HashMap<>();
    private static final Map<Integer, BlockFace> FROM_ID = new HashMap<>();

    static
    {
        for (final BlockFace face : BlockFace.values())
        {
            FROM_DIRECTION.put(face.directionVector, face);
            FROM_ID.put(face.val, face);
        }
    }

    /**
     * The vector of this {@link BlockFace}. For example, {@link BlockFace#UP} would be (0,1,0), as it's direction is
     * positive in the y-axis and 0 in every other direction.
     */
    private final Vector3Di directionVector;
    private final int val;

    BlockFace(int val, Vector3Di directionVector)
    {
        this.val = val;
        this.directionVector = directionVector;
    }

    /**
     * Get the {@link BlockFace} that's the exact opposite of the provided one. For example, the opposite side of
     * {@link BlockFace#UP} is {@link BlockFace#DOWN}.
     *
     * @param dir
     *     The current {@link BlockFace}
     * @return The opposite direction of the current {@link BlockFace}.
     */
    @SuppressWarnings("unused")
    public static BlockFace getOpposite(BlockFace dir)
    {
        return switch (dir)
        {
            case DOWN -> BlockFace.UP;
            case EAST -> BlockFace.WEST;
            case NORTH -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.NORTH;
            case UP -> BlockFace.DOWN;
            case WEST -> BlockFace.EAST;
            default -> BlockFace.NONE;
        };
    }

    /**
     * Gets the integer value of a {@link BlockFace}.
     *
     * @param dir
     *     The {@link BlockFace}.
     * @return The integer value of a {@link BlockFace}.
     */
    public static int getValue(BlockFace dir)
    {
        return dir.val;
    }

    @SuppressWarnings("unused")
    public static BlockFace valueOf(int val)
    {
        return Objects.requireNonNull(FROM_ID.get(val), "Could not find mapping for BlockFace with id " + val);
    }

    /**
     * Get the {@link BlockFace#directionVector} of this {@link BlockFace}
     *
     * @param myFace
     *     The direction.
     * @return The vector of the direction.
     */
    public static Vector3Di getDirection(BlockFace myFace)
    {
        return myFace.directionVector;
    }

    /**
     * Rotate the {@link BlockFace} in clockwise direction from a perspective of looking down on the world. For example,
     * {@link BlockFace#NORTH} would return {@link BlockFace#EAST}.
     *
     * @param myFace
     *     The current {@link BlockFace}.
     * @return The rotated {@link BlockFace}.
     */
    public static BlockFace rotateClockwise(BlockFace myFace)
    {
        return switch (myFace)
        {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> myFace;
        };
    }

    /**
     * Rotate the {@link BlockFace} in counterclockwise direction from a perspective of looking down on the world. For
     * example, {@link BlockFace#NORTH} would return {@link BlockFace#WEST}.
     *
     * @param myFace
     *     The current {@link BlockFace}.
     * @return The rotated {@link BlockFace}.
     */
    public static BlockFace rotateCounterClockwise(BlockFace myFace)
    {
        return switch (myFace)
        {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            default -> myFace;
        };
    }

    /**
     * Rotate the {@link BlockFace} in northern direction on the vertical plane. For example, {@link BlockFace#UP} would
     * return {@link BlockFace#NORTH} and {@link BlockFace#NORTH} would return {@link BlockFace#DOWN}.
     *
     * @param curFace
     *     The current {@link BlockFace}.
     * @return The rotated {@link BlockFace}.
     */
    public static BlockFace rotateVerticallyNorth(BlockFace curFace)
    {
        return switch (curFace)
        {
            case DOWN -> BlockFace.SOUTH;
            case NORTH -> BlockFace.DOWN;
            case SOUTH -> BlockFace.UP;
            case UP -> BlockFace.NORTH;
            default -> curFace;
        };
    }

    /**
     * Rotate the {@link BlockFace} in southern direction on the vertical plane. For example, {@link BlockFace#UP} would
     * return {@link BlockFace#SOUTH} and {@link BlockFace#SOUTH} would return {@link BlockFace#DOWN}.
     *
     * @param curFace
     *     The current {@link BlockFace}.
     * @return The rotated {@link BlockFace}.
     */
    public static BlockFace rotateVerticallySouth(BlockFace curFace)
    {
        return switch (curFace)
        {
            case DOWN -> BlockFace.NORTH;
            case NORTH -> BlockFace.UP;
            case SOUTH -> BlockFace.DOWN;
            case UP -> BlockFace.SOUTH;
            default -> curFace;
        };
    }

    /**
     * Rotate the {@link BlockFace} in eastern direction on the vertical plane. For example, {@link BlockFace#UP} would
     * return {@link BlockFace#EAST} and {@link BlockFace#EAST} would return {@link BlockFace#DOWN}.
     *
     * @param curFace
     *     The current {@link BlockFace}.
     * @return The rotated {@link BlockFace}.
     */
    public static BlockFace rotateVerticallyEast(BlockFace curFace)
    {
        return switch (curFace)
        {
            case DOWN -> BlockFace.WEST;
            case EAST -> BlockFace.DOWN;
            case WEST -> BlockFace.UP;
            case UP -> BlockFace.EAST;
            default -> curFace;
        };
    }

    /**
     * Rotate the {@link BlockFace} in western direction on the vertical plane. For example, {@link BlockFace#UP} would
     * return {@link BlockFace#WEST} and {@link BlockFace#WEST} would return {@link BlockFace#DOWN}.
     *
     * @param curFace
     *     The current {@link BlockFace}.
     * @return The rotated {@link BlockFace}.
     */
    public static BlockFace rotateVerticallyWest(BlockFace curFace)
    {
        return switch (curFace)
        {
            case DOWN -> BlockFace.EAST;
            case EAST -> BlockFace.UP;
            case WEST -> BlockFace.DOWN;
            case UP -> BlockFace.WEST;
            default -> curFace;
        };
    }

    /**
     * Get the blockFace from a {@link BlockFace#directionVector} value.
     *
     * @param dir
     *     The {@link BlockFace#directionVector}.
     * @return The {@link BlockFace} associated with this {@link BlockFace#directionVector}.
     */
    @SuppressWarnings("unused")
    public static Optional<BlockFace> faceFromDir(Vector3Di dir)
    {
        return Optional.ofNullable(FROM_DIRECTION.get(dir));
    }

    /**
     * Get the appropriate function for rotating a BlockFace. Different rotation directions use different methods.
     *
     * @param movementDirection
     *     The {@link MovementDirection} to rotate in.
     * @return The appropriate function for rotating the {@link BlockFace} in the given direction.
     */
    public static @Nullable UnaryOperator<BlockFace> getRotationFunction(MovementDirection movementDirection)
    {
        return switch (movementDirection)
        {
            case NORTH -> BlockFace::rotateVerticallyNorth;
            case EAST -> BlockFace::rotateVerticallyEast;
            case SOUTH -> BlockFace::rotateVerticallySouth;
            case WEST -> BlockFace::rotateVerticallyWest;
            case CLOCKWISE -> BlockFace::rotateClockwise;
            case COUNTERCLOCKWISE -> BlockFace::rotateCounterClockwise;
            default -> null;
        };
    }

    /**
     * Rotate a block face in a given direction for a number of steps.
     *
     * @param blockFace
     *     The {@link BlockFace} that will be rotated.
     * @param steps
     *     The number of times to apply the rotation.
     * @param rotationFunction
     *     The function the applies the rotation.
     * @return The rotated {@link BlockFace}.
     *
     * @see BlockFace#getRotationFunction
     */
    public static BlockFace rotate(BlockFace blockFace, int steps, @Nullable UnaryOperator<BlockFace> rotationFunction)
    {
        if (rotationFunction == null || blockFace.equals(BlockFace.NONE))
            return blockFace;

        // Every 4 steps results in the same outcome.
        int realSteps = steps % 4;
        if (realSteps == 0)
            return blockFace;

        BlockFace newFace = blockFace;
        while (realSteps-- > 0)
            newFace = rotationFunction.apply(newFace);
        return newFace;
    }

    /**
     * Rotate a block face in a given direction for a number of steps.
     *
     * @param blockFace
     *     The {@link BlockFace} that will be rotated.
     * @param steps
     *     The number of times to apply the rotation.
     * @param direction
     *     The direction to rotate in.
     * @return The rotated {@link BlockFace}.
     */
    public static BlockFace rotate(BlockFace blockFace, int steps, MovementDirection direction)
    {
        return rotate(blockFace, steps, getRotationFunction(direction));
    }
}
