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
     * The name used by all entities created by AnimatedArchitecture.
     */
    public static final String ANIMATED_ARCHITECTURE_ENTITY_NAME = "AnimatedArchitectureEntity";

    /**
     * The amount of time (in ms) a user gets to complete a command waiter.
     */
    public static final int COMMAND_WAITER_TIMEOUT = 60 * 1000;

    /**
     * Whether the current build is a dev build. Certain options are enabled, disabled, or overridden depending on this
     * variable.
     */
    public static final boolean DEV_BUILD = true;

    /**
     * Minimum number of ticks a structure needs to cool down before it can be toggled again. This should help with some
     * rare cases of overlapping processes and whatnot.
     */
    public static final int MINIMUM_STRUCTURE_DELAY = 10;

    /**
     * The name of this plugin.
     */
    public static final String PLUGIN_NAME = "AnimatedArchitecture";

    /**
     * The directory where all the extensions are loaded from.
     */
    public static final String ANIMATE_ARCHITECTURE_EXTENSIONS_FOLDER_NAME = "extensions";

    /**
     * The amount of time (in seconds) a user has to complete the structure creation process.
     */
    public static final int STRUCTURE_CREATOR_TIME_LIMIT = 120 * 20;

    /**
     * The prefix for the {@link StructureAttribute}-specific bypass permissions.
     */
    public static final String ATTRIBUTE_BYPASS_PERMISSION_PREFIX = "animatedarchitecture.admin.bypass.attribute.";
}
