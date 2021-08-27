package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents all directions a door can rotate in.
 *
 * @author Pim
 */
public enum RotateDirection
{
    NONE(0, "constants.rotate_direction.none"),
    CLOCKWISE(1, "constants.rotate_direction.clockwise"),
    COUNTERCLOCKWISE(2, "constants.rotate_direction.counterclockwise"),
    UP(3, "constants.rotate_direction.up"),
    DOWN(4, "constants.rotate_direction.down"),
    NORTH(5, "constants.rotate_direction.north"),
    EAST(6, "constants.rotate_direction.east"),
    SOUTH(7, "constants.rotate_direction.south"),
    WEST(8, "constants.rotate_direction.west");

    /**
     * Map of all indices with their respective {@link RotateDirection} constants as values.
     */
    private static final Map<Integer, RotateDirection> idMap;
    private static final Map<String, RotateDirection> nameMap;

    static
    {
        final RotateDirection[] values = RotateDirection.values();
        final Map<Integer, RotateDirection> idMapTmp = new HashMap<>(values.length);
        final Map<String, RotateDirection> nameMapTmp = new HashMap<>(values.length);
        for (final RotateDirection dir : RotateDirection.values())
        {
            idMapTmp.put(dir.val, dir);
            nameMapTmp.put(dir.name(), dir);
        }
        idMap = Collections.unmodifiableMap(idMapTmp);
        nameMap = Collections.unmodifiableMap(nameMapTmp);
    }

    private final int val;

    @Getter
    private final String localizationKey;

    RotateDirection(int val, String localizationKey)
    {
        this.val = val;
        this.localizationKey = localizationKey;
    }

    /**
     * Gets the index value of a {@link RotateDirection}.
     *
     * @param dir
     *     The {@link RotateDirection}.
     * @return The index value of a {@link RotateDirection}.
     */
    public static int getValue(RotateDirection dir)
    {
        return dir.val;
    }

    /**
     * Gets the {@link RotateDirection} from an index value.
     *
     * @param dir
     *     The {@link RotateDirection}.
     * @return The {@link RotateDirection} associated with this index value.
     */
    public static @Nullable RotateDirection valueOf(int dir)
    {
        try
        {
            return idMap.get(dir);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static Optional<RotateDirection> getRotateDirection(String name)
    {
        return Optional.ofNullable(nameMap.get(name));
    }

    /**
     * Get the {@link RotateDirection} that's the exact opposite of the provided one. For example, the opposite side of
     * {@link RotateDirection#UP} is {@link RotateDirection#DOWN}.
     *
     * @param dir
     *     The current {@link RotateDirection}
     * @return The opposite direction of the current {@link RotateDirection}.
     */
    public static RotateDirection getOpposite(RotateDirection dir)
    {
        switch (dir)
        {
            case DOWN:
                return RotateDirection.UP;
            case EAST:
                return RotateDirection.WEST;
            case NORTH:
                return RotateDirection.SOUTH;
            case SOUTH:
                return RotateDirection.NORTH;
            case UP:
                return RotateDirection.DOWN;
            case WEST:
                return RotateDirection.EAST;
            case CLOCKWISE:
                return RotateDirection.COUNTERCLOCKWISE;
            case COUNTERCLOCKWISE:
                return RotateDirection.CLOCKWISE;
            case NONE:
            default:
                return RotateDirection.NONE;
        }
    }
}
