package nl.pim16aap2.bigdoors.doors;


import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Represent the possible attributes of a door.
 *
 * @author Pim
 */
public enum DoorAttribute
{
    /**
     * (Un)lock the door.
     */
    LOCK(PermissionLevel.USER),

    /**
     * Toggle the door. If it is currently opened, it will be closed. If it is currently closed, it will be opened
     * instead.
     */
    TOGGLE(PermissionLevel.USER),

    /**
     * Turns a door on or off. When on, the door will rotate until turned off or until the chunks are unloaded.
     */
    SWITCH(PermissionLevel.USER),

    /**
     * Get the info of the door.
     */
    INFO(PermissionLevel.USER),

    /**
     * Delete the door.
     */
    DELETE(PermissionLevel.CREATOR),

    /**
     * Relocate the power block.
     */
    RELOCATE_POWERBLOCK(PermissionLevel.ADMIN),

    /**
     * The auto close timer of this door. i.e. the amount of time it will wait before automatically closing.
     */
    AUTO_CLOSE_TIMER(PermissionLevel.ADMIN),

    /**
     * The open direction of a door.
     */
    OPEN_DIRECTION(PermissionLevel.USER),

    /**
     * The number of blocks an animated will try to move.
     */
    BLOCKS_TO_MOVE(PermissionLevel.ADMIN),

    /**
     * Add an owner.
     */
    ADD_OWNER(PermissionLevel.ADMIN),

    /**
     * Remove an owner.
     */
    REMOVE_OWNER(PermissionLevel.ADMIN);

    private static final List<DoorAttribute> VALUES = Arrays.asList(values());

    /**
     * The minimum level of ownership required to access an attribute.
     */
    @Getter
    private final PermissionLevel permissionLevel;

    DoorAttribute(PermissionLevel permissionLevel)
    {
        this.permissionLevel = permissionLevel;
    }

    /**
     * Checks if a given permission level has access to this attribute.
     *
     * @param permissionLevel
     *     The permission level to check.
     * @return True if the given permission level has access to this attribute.
     */
    public boolean canAccessWith(PermissionLevel permissionLevel)
    {
        return permissionLevel.isLowerThanOrEquals(getPermissionLevel());
    }

    /**
     * @return All values in this enum as an unmodifiable list.
     * <p>
     * Unlike {@link #values()}, this does not create a new object.
     */
    public static List<DoorAttribute> getValues()
    {
        return VALUES;
    }
}
