package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represents the access level of a property.
 * <p>
 * This determines to what extent a user can interact with the property.
 */
public enum PropertyAccessLevel
{
    /**
     * The property is hidden from the user.
     * <p>
     * It will not be shown in the GUI or in any other user-facing interface.
     * <p>
     * This is useful for properties that are only used strictly internally.
     */
    NONE(0),

    /**
     * The property is visible to the user but not directly editable.
     * <p>
     * It may be shown in the GUI and in any other user-facing interface.
     * <p>
     * This is useful for properties that the user should be able to see but not change directly.
     */
    READ(1),

    /**
     * The property is editable by the user.
     * <p>
     * The user can directly change the value of this property.
     */
    EDIT(1 << 1),

    /**
     * The user can add this property to structures that do not yet have it.
     */
    ADD(1 << 2),

    /**
     * The user can remove this property from structures that have it.
     */
    REMOVE(1 << 3),
    ;

    /**
     * A list of all values in this enum.
     */
    public static final List<PropertyAccessLevel> VALUES = Arrays.asList(PropertyAccessLevel.values());

    @Getter
    private final int flag;

    PropertyAccessLevel(int flag)
    {
        this.flag = flag;
    }

    /**
     * Checks if the given permission flag has all the given access levels.
     *
     * @param permissionFlag
     *     The permission flag to check.
     * @param levels
     *     The access levels to check for.
     * @return True if the permission flag has all the given access levels, false otherwise.
     */
    public static boolean hasFlags(int permissionFlag, PropertyAccessLevel... levels)
    {
        return hasFlags(permissionFlag, Arrays.asList(levels));
    }

    /**
     * Checks if the given permission flag has all the given access levels.
     *
     * @param permissionFlag
     *     The permission flag to check.
     * @param levels
     *     The access levels to check for.
     * @return True if the permission flag has all the given access levels, false otherwise.
     */
    public static boolean hasFlags(int permissionFlag, Collection<PropertyAccessLevel> levels)
    {
        for (final PropertyAccessLevel level : levels)
        {
            if (!hasFlag(permissionFlag, level))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given permission flag has at least one of the given access levels.
     *
     * @param permissionFlag
     *     The permission flag to check.
     * @param levels
     *     The access levels to check for.
     * @return True if the permission flag has at least one of the given access levels, false otherwise.
     */
    public static boolean hasOneFlagOf(int permissionFlag, PropertyAccessLevel... levels)
    {
        return hasOneFlagOf(permissionFlag, Arrays.asList(levels));
    }

    /**
     * Checks if the given permission flag has at least one of the given access levels.
     *
     * @param permissionFlag
     *     The permission flag to check.
     * @param levels
     *     The access levels to check for.
     * @return True if the permission flag has at least one of the given access levels, false otherwise.
     */
    public static boolean hasOneFlagOf(int permissionFlag, Collection<PropertyAccessLevel> levels)
    {
        for (final PropertyAccessLevel level : levels)
        {
            if (hasFlag(permissionFlag, level))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given permission flag has the given access level.
     *
     * @param permissionFlag
     *     The permission flag to check.
     * @param level
     *     The access level to check for.
     * @return True if the permission flag has the given access level, false otherwise.
     */
    public static boolean hasFlag(int permissionFlag, PropertyAccessLevel level)
    {
        return (permissionFlag & level.getFlag()) != 0;
    }

    /**
     * Returns the combined permission flag of the given access levels.
     *
     * @param levels
     *     The access levels to combine.
     * @return The combined permission flag.
     */
    public static int getFlagOf(PropertyAccessLevel... levels)
    {
        return getFlagOf(Arrays.asList(levels));
    }

    /**
     * Returns the combined permission flag of the given access levels.
     *
     * @param levels
     *     The access levels to combine.
     * @return The combined permission flag.
     */
    public static int getFlagOf(Collection<PropertyAccessLevel> levels)
    {
        int flag = 0;
        for (final PropertyAccessLevel level : levels)
        {
            flag |= level.getFlag();
        }
        return flag;
    }
}
