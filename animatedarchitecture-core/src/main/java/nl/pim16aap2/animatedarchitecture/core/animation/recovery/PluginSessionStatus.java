package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

/**
 * Represents the lifecycle state of an AnimatedArchitecture plugin session.
 */
public enum PluginSessionStatus
{
    /**
     * The plugin session is currently active.
     */
    ACTIVE,

    /**
     * The plugin session ended through the normal shutdown path.
     */
    CLEAN,

    /**
     * The plugin session did not record a normal shutdown before a later session started.
     */
    UNCLEAN,
}
