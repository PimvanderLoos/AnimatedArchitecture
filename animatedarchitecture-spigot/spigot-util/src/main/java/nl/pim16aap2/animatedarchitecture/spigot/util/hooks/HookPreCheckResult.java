package nl.pim16aap2.animatedarchitecture.spigot.util.hooks;

/**
 * Represents the result of a protection hook check.
 */
public enum HookPreCheckResult
{
    /**
     * The check denied the action.
     */
    DENY,

    /**
     * The check allowed the action. This does not mean that the action will be allowed, as other checks may still deny
     * the action.
     */
    ALLOW,

    /**
     * The check is bypassed. This means that the check is not run at all.
     */
    BYPASS,
    ;
}
