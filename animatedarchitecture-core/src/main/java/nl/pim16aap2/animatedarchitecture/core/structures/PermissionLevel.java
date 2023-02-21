package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a permission level where a lower level indicates a higher access level.
 */
@ToString
public enum PermissionLevel
{
    CREATOR(0, "constants.permission_level.creator"),
    ADMIN(100, "constants.permission_level.admin"),
    USER(200, "constants.permission_level.user"),
    NO_PERMISSION(999, "constants.permission_level.no_permission"),
    ;

    private static final List<PermissionLevel> VALUES = List.of(values());

    private static final Map<Integer, PermissionLevel> VALUE_MAP =
        VALUES.stream().collect(Collectors.toUnmodifiableMap(PermissionLevel::getValue, Function.identity()));

    private static final Map<String, PermissionLevel> NAME_MAP =
        VALUES.stream().collect(Collectors.toUnmodifiableMap(PermissionLevel::name, Function.identity()));

    @Getter
    private final int value;

    @Getter
    private final String translationKey;

    PermissionLevel(int value, String translationKey)
    {
        this.value = value;
        this.translationKey = translationKey;
    }

    /**
     * Retrieves the {@link PermissionLevel} mapped to a given id.
     *
     * @param id
     *     The id of the structure permission to retrieve.
     * @return The structure permission with the given id if it exists, otherwise null.
     */
    public static @Nullable PermissionLevel fromValue(int id)
    {
        return VALUE_MAP.get(id);
    }

    /**
     * Retrieves the {@link PermissionLevel} from its name.
     * <p>
     * This method is case-insensitive.
     *
     * @param name
     *     The name of the structure permission to retrieve.
     * @return The structure permission with the given name if it exists, otherwise null.
     */
    public static @Nullable PermissionLevel fromName(String name)
    {
        return NAME_MAP.get(name.toLowerCase(Locale.ROOT));
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

    /**
     * Retrieves an unmodifiable list of all values.
     * <p>
     * See {@link #values()}.
     *
     * @return The values in this enum.
     */
    public static List<PermissionLevel> getValues()
    {
        return VALUES;
    }
}
