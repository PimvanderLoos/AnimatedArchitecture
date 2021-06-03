package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
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
    NONE(0, Message.GENERAL_DIRECTION_NONE),
    CLOCKWISE(1, Message.GENERAL_DIRECTION_CLOCKWISE),
    COUNTERCLOCKWISE(2, Message.GENERAL_DIRECTION_COUNTERCLOCKWISE),
    UP(3, Message.GENERAL_DIRECTION_UP),
    DOWN(4, Message.GENERAL_DIRECTION_DOWN),
    NORTH(5, Message.GENERAL_DIRECTION_NORTH),
    EAST(6, Message.GENERAL_DIRECTION_EAST),
    SOUTH(7, Message.GENERAL_DIRECTION_SOUTH),
    WEST(8, Message.GENERAL_DIRECTION_WEST);

    /**
     * Map of all indices with their respective {@link RotateDirection} constants as values.
     */
    private final static @NotNull Map<Integer, RotateDirection> idMap;
    private final static @NotNull Map<String, RotateDirection> nameMap;

    static
    {
        final @NotNull RotateDirection[] values = RotateDirection.values();
        final @NotNull Map<Integer, RotateDirection> idMapTmp = new HashMap<>(values.length);
        final @NotNull Map<String, RotateDirection> nameMapTmp = new HashMap<>(values.length);
        for (final @NotNull RotateDirection dir : RotateDirection.values())
        {
            idMapTmp.put(dir.val, dir);
            nameMapTmp.put(dir.name(), dir);
        }
        idMap = Collections.unmodifiableMap(idMapTmp);
        nameMap = Collections.unmodifiableMap(nameMapTmp);
    }

    private final int val;

    @Getter

    private final @NotNull Message message;

    RotateDirection(final int val, final @NotNull Message message)
    {
        this.val = val;
        this.message = message;
    }

    /**
     * Gets the index value of a {@link RotateDirection}.
     *
     * @param dir The {@link RotateDirection}.
     * @return The index value of a {@link RotateDirection}.
     */
    public static int getValue(final @NotNull RotateDirection dir)
    {
        return dir.val;
    }

    /**
     * Gets the {@link RotateDirection} from an index value.
     *
     * @param dir The {@link RotateDirection}.
     * @return The {@link RotateDirection} associated with this index value.
     */
    public static @Nullable RotateDirection valueOf(final int dir)
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

    public static @NotNull Optional<RotateDirection> getRotateDirection(final @NotNull String name)
    {
        return Optional.ofNullable(nameMap.get(name));
    }

    /**
     * Gets the {@link Message} associated with a {@link RotateDirection}.
     *
     * @param dir The {@link RotateDirection}.
     * @return The {@link Message} associated with a {@link RotateDirection}.
     */
    public static @NotNull Message getMessage(final @NotNull RotateDirection dir)
    {
        return dir.message;
    }

    /**
     * Get the {@link RotateDirection} that's the exact opposite of the provided one. For example, the opposite side of
     * {@link RotateDirection#UP} is {@link RotateDirection#DOWN}.
     *
     * @param dir The current {@link RotateDirection}
     * @return The opposite direction of the current {@link RotateDirection}.
     */
    public static @NotNull RotateDirection getOpposite(final @NotNull RotateDirection dir)
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
