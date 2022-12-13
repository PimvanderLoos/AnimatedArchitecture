package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a permission level where a lower level indicates a higher access level.
 */
@ToString
public enum PermissionLevel
{
    CREATOR(0),
    ADMIN(100),
    USER(200),
    NO_PERMISSION(999),
    ;

    private static final Map<Integer, PermissionLevel> VALUE_MAP =
        Stream.of(PermissionLevel.values())
              .collect(Collectors.toUnmodifiableMap(PermissionLevel::getValue, Function.identity()));

    @Getter
    private final int value;

    PermissionLevel(int value)
    {
        this.value = value;
    }

    /**
     * Retrieves the {@link PermissionLevel} mapped to a given id.
     *
     * @param id
     *     The id of the door permission to retrieve.
     * @return The door permission with the given id if it exists, otherwise null.
     */
    public static @Nullable PermissionLevel fromValue(int id)
    {
        return VALUE_MAP.get(id);
    }

    /**
     * Checks if the current permission is lower than or equal to the other permission level.
     *
     * @param other
     *     The other permission level to compare against.
     * @return True if the current permission level is lower than or equal to the other permission level, otherwise
     * false.
     */
    public boolean isLowerThanOrEquals(PermissionLevel other)
    {
        return this.value <= other.value;
    }

    /**
     * Checks if the current permission is lower than other permission level.
     *
     * @param other
     *     The other permission level to compare against.
     * @return True if the current permission level is lower than the other permission level, otherwise false.
     */
    public boolean isLowerThan(PermissionLevel other)
    {
        return this.value < other.value;
    }
}
