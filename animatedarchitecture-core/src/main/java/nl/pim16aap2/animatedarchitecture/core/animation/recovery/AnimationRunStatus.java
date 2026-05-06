package nl.pim16aap2.animatedarchitecture.core.animation.recovery;

/**
 * Represents the lifecycle state of a single animation run.
 */
public enum AnimationRunStatus
{
    /**
     * The animation run is currently active.
     */
    ACTIVE,

    /**
     * The animation run completed through the normal path (or was successfully aborted).
     */
    COMPLETED,

    /**
     * The animation run failed before it could complete normally.
     */
    FAILED,
}
