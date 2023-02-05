package nl.pim16aap2.bigdoors.core.util;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents all possible movement directions of a Structure.
 *
 * @author Pim
 */
public enum MovementDirection
{
    NONE(0, "constants.movement_direction.none"),
    CLOCKWISE(1, "constants.movement_direction.clockwise"),
    COUNTERCLOCKWISE(2, "constants.movement_direction.counterclockwise"),
    UP(3, "constants.movement_direction.up"),
    DOWN(4, "constants.movement_direction.down"),
    NORTH(5, "constants.movement_direction.north"),
    EAST(6, "constants.movement_direction.east"),
    SOUTH(7, "constants.movement_direction.south"),
    WEST(8, "constants.movement_direction.west");

    /**
     * Map of all indices with their respective {@link MovementDirection} constants as values.
     */
    private static final Map<Integer, MovementDirection> ID_MAP;
    private static final Map<String, MovementDirection> NAME_MAP;

    static
    {
        final MovementDirection[] values = MovementDirection.values();
        final Map<Integer, MovementDirection> idMapTmp = new HashMap<>(values.length);
        final Map<String, MovementDirection> nameMapTmp = new HashMap<>(values.length);
        for (final MovementDirection dir : MovementDirection.values())
        {
            idMapTmp.put(dir.val, dir);
            nameMapTmp.put(dir.name(), dir);
        }
        ID_MAP = Collections.unmodifiableMap(idMapTmp);
        NAME_MAP = Collections.unmodifiableMap(nameMapTmp);
    }

    private final int val;

    @Getter
    private final String localizationKey;

    MovementDirection(int val, String localizationKey)
    {
        this.val = val;
        this.localizationKey = localizationKey;
    }

    /**
     * Gets the index value of a {@link MovementDirection}.
     *
     * @param dir
     *     The {@link MovementDirection}.
     * @return The index value of a {@link MovementDirection}.
     */
    public static int getValue(MovementDirection dir)
    {
        return dir.val;
    }

    /**
     * Gets the {@link MovementDirection} from an index value.
     *
     * @param dir
     *     The {@link MovementDirection}.
     * @return The {@link MovementDirection} associated with this index value.
     */
    public static @Nullable MovementDirection valueOf(int dir)
    {
        try
        {
            return ID_MAP.get(dir);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static Optional<MovementDirection> getMovementDirection(String name)
    {
        return Optional.ofNullable(NAME_MAP.get(name));
    }

    /**
     * Get the {@link MovementDirection} that's the exact opposite of the provided one. For example, the opposite side
     * of {@link MovementDirection#UP} is {@link MovementDirection#DOWN}.
     *
     * @param dir
     *     The current {@link MovementDirection}
     * @return The opposite direction of the current {@link MovementDirection}.
     */
    public static MovementDirection getOpposite(MovementDirection dir)
    {
        return switch (dir)
            {
                case DOWN -> MovementDirection.UP;
                case EAST -> MovementDirection.WEST;
                case NORTH -> MovementDirection.SOUTH;
                case SOUTH -> MovementDirection.NORTH;
                case UP -> MovementDirection.DOWN;
                case WEST -> MovementDirection.EAST;
                case CLOCKWISE -> MovementDirection.COUNTERCLOCKWISE;
                case COUNTERCLOCKWISE -> MovementDirection.CLOCKWISE;
                default -> MovementDirection.NONE;
            };
    }
}
