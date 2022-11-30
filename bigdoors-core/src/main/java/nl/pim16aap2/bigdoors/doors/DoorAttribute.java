package nl.pim16aap2.bigdoors.doors;


import lombok.Getter;

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
    LOCK(2),

    /**
     * Toggle the door. If it is currently opened, it will be closed. If it is currently closed, it will be opened
     * instead.
     */
    TOGGLE(2),

    /**
     * Turns a door on or off. When on, the door will rotate until turned off or until the chunks are unloaded.
     */
    SWITCH(2),

    /**
     * Get the info of the door.
     */
    INFO(2),

    /**
     * Delete the door.
     */
    DELETE(0),

    /**
     * Relocate the power block.
     */
    RELOCATE_POWERBLOCK(1),

    /**
     * The auto close timer of this door. i.e. the amount of time it will wait before automatically closing.
     */
    AUTO_CLOSE_TIMER(1),

    /**
     * The open direction of a door.
     */
    OPEN_DIRECTION(2),

    /**
     * The number of blocks an animated will try to move.
     */
    BLOCKS_TO_MOVE(1),

    /**
     * Add an owner.
     */
    ADD_OWNER(1),

    /**
     * Remove an owner.
     */
    REMOVE_OWNER(1);

    /**
     * The minimum level of ownership (0 = highest) required to access an attribute.
     */
    @Getter
    private final int permissionLevel;

    DoorAttribute(int permissionLevel)
    {
        this.permissionLevel = permissionLevel;
    }

    /**
     * Gets the minimum level of ownership (0 = highest) required to access a door's attribute.
     *
     * @param atr
     *     The attribute for which the permission level will be retrieved.
     * @return The minimum level of ownership (0 = highest) required to access a door's attribute.
     */
    public static int getPermissionLevel(DoorAttribute atr)
    {
        return atr.permissionLevel;
    }
}
