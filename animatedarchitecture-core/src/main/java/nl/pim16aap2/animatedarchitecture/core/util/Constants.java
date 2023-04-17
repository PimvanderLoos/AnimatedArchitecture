package nl.pim16aap2.animatedarchitecture.core.util;


import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;

/**
 * Represents a list of constant variables.
 *
 * @author Pim
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
     * The name used by all entities created by AnimatedArchitecture.
     */
    public static final String ANIMATED_ARCHITECTURE_ENTITY_NAME = "AnimatedArchitectureEntity";

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
    public static final int STRUCTURE_CREATOR_TIME_LIMIT = 180_000; // 3 minutes

    /**
     * The prefix for the {@link StructureAttribute}-specific bypass permissions.
     */
    public static final String ATTRIBUTE_BYPASS_PERMISSION_PREFIX = "animatedarchitecture.admin.bypass.attribute.";
}
