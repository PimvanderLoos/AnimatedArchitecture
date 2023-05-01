package nl.pim16aap2.animatedarchitecture.core.structures;


import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Represent the possible attributes of a structure.
 *
 * @author Pim
 */
public enum StructureAttribute
{
    /**
     * Add an owner.
     */
    ADD_OWNER(PermissionLevel.ADMIN),

    /**
     * The number of blocks an animated will try to move.
     */
    BLOCKS_TO_MOVE(PermissionLevel.ADMIN),

    /**
     * Delete the structure.
     */
    DELETE(PermissionLevel.CREATOR),

    /**
     * Get the info of the structure.
     */
    INFO(PermissionLevel.USER),

    /**
     * (Un)lock the structure.
     */
    LOCK(PermissionLevel.USER),

    /**
     * The open direction of a structure.
     */
    OPEN_DIRECTION(PermissionLevel.USER),

    /**
     * The open status of a structure.
     */
    OPEN_STATUS(PermissionLevel.USER),

    /**
     * Show a preview of the animation.
     */
    PREVIEW(PermissionLevel.USER),

    /**
     * Relocate the power block.
     */
    RELOCATE_POWERBLOCK(PermissionLevel.ADMIN),

    /**
     * Remove an owner.
     */
    REMOVE_OWNER(PermissionLevel.ADMIN),

    /**
     * Turns a structure on or off. When on, the structure will rotate until turned off or until the chunks are
     * unloaded.
     */
    SWITCH(PermissionLevel.USER),

    /**
     * Toggle the structure. If it is currently opened, it will be closed. If it is currently closed, it will be opened
     * instead.
     */
    TOGGLE(PermissionLevel.USER);

    private static final List<StructureAttribute> VALUES = Arrays.asList(values());

    /**
     * The minimum level of ownership required to access an attribute.
     */
    @Getter
    private final PermissionLevel permissionLevel;

    /**
     * The admin permission node for this attribute.
     */
    @Getter
    private final String adminPermissionNode;

    StructureAttribute(PermissionLevel permissionLevel)
    {
        this.permissionLevel = permissionLevel;
        this.adminPermissionNode = getAdminPermissionNode(this);
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
    public static List<StructureAttribute> getValues()
    {
        return VALUES;
    }

    /**
     * Gets the admin permission node for a given {@link StructureAttribute}.
     * <p>
     * Consider using {@link StructureAttribute#getAdminPermissionNode()} instead, as it uses the cached value.
     *
     * @param structureAttribute
     *     The {@link StructureAttribute} to get the admin permission node for.
     * @return The admin permission node for the given {@link StructureAttribute}.
     */
    public static String getAdminPermissionNode(StructureAttribute structureAttribute)
    {
        return Constants.PERMISSION_PREFIX_ADMIN_BYPASS_ATTRIBUTE +
            structureAttribute.name().replace("_", "").toLowerCase(Locale.ROOT);
    }
}
