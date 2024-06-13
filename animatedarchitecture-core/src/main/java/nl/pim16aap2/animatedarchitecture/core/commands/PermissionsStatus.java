package nl.pim16aap2.animatedarchitecture.core.commands;

/**
 * Represents a pair of booleans that determine whether a user has user-level and/or admin-level permission access to
 * some kind of action.
 */
public record PermissionsStatus(boolean hasUserPermission, boolean hasAdminPermission)
{
    /**
     * Checks if either the user-level or the admin-level permission is available.
     *
     * @return True if either {@link #hasUserPermission} or {@link #hasAdminPermission} is true.
     */
    public boolean hasAnyPermission()
    {
        return hasUserPermission || hasAdminPermission;
    }
}
