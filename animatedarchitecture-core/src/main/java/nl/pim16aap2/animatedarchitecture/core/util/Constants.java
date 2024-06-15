package nl.pim16aap2.animatedarchitecture.core.util;


/**
 * Represents a list of constant variables.
 */
public final class Constants
{
    /**
     * The prefix of all permissions that a regular user should have access to for normal usage.
     * <p>
     * This will include stuff like creating new structures and managing structures that you own.
     */
    public static final String PERMISSION_PREFIX_USER = "animatedarchitecture.user.";

    /**
     * The prefix of all permissions for admin-related actions.
     * <p>
     * This should include stuff like affecting server settings and managing structures owned by other players.
     */
    public static final String PERMISSION_PREFIX_ADMIN = "animatedarchitecture.admin.";

    /**
     * A specialization of {@link #PERMISSION_PREFIX_ADMIN} for bypass permission nodes.
     */
    public static final String PERMISSION_PREFIX_ADMIN_BYPASS = PERMISSION_PREFIX_ADMIN + "bypass.";

    /**
     * A specialization of {@link #PERMISSION_PREFIX_ADMIN} for bypass permission nodes for attributes.
     */
    public static final String PERMISSION_PREFIX_ADMIN_BYPASS_ATTRIBUTE = PERMISSION_PREFIX_ADMIN_BYPASS + "attribute.";

    /**
     * A specialization of {@link #PERMISSION_PREFIX_ADMIN} for bypass permission nodes for limits.
     */
    public static final String PERMISSION_PREFIX_ADMIN_BYPASS_LIMIT = PERMISSION_PREFIX_ADMIN_BYPASS + "limit.";

    /**
     * The key used to store the recovery data of an animated block in the metadata of an animated block.
     */
    public static final String ANIMATED_ARCHITECTURE_ENTITY_RECOVERY_KEY = "AnimatedArchitectureEntity";

    /**
     * The amount of time (in ms) a user gets to complete a command waiter.
     */
    public static final int COMMAND_WAITER_TIMEOUT = 60_000;

    /**
     * The name of this plugin.
     */
    public static final String PLUGIN_NAME = "AnimatedArchitecture";

    /**
     * The directory where all the extensions are loaded from.
     */
    public static final String ANIMATE_ARCHITECTURE_EXTENSIONS_FOLDER_NAME = "extensions";

    /**
     * The amount of time (in milliseconds) a user has to complete the structure creation process.
     */
    public static final int STRUCTURE_CREATOR_TIME_LIMIT = 900_000; // 15 minutes
}
