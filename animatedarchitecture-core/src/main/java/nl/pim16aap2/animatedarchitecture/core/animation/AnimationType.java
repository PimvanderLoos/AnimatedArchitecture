package nl.pim16aap2.animatedarchitecture.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;

/**
 * Represents several types of animation.
 */
public enum AnimationType
{
    /**
     * Animates the movement of blocks from their starting position to their final position, which may be somewhere
     * else.
     */
    MOVE_BLOCKS(true, true, Double.MAX_VALUE),

    /**
     * Animates a preview of an animation. No blocks are affected in the world.
     * <p>
     * Note that this type requires an online {@link IPlayer} to target.
     */
    PREVIEW(false, false, 10),
    ;

    private final boolean requiresWriteAccess;
    private final boolean supportsPerpetualAnimation;
    private final double animationDurationLimit;

    AnimationType(
        boolean requiresWriteAccess, boolean supportsPerpetualAnimation, double animationDurationLimit)
    {
        this.requiresWriteAccess = requiresWriteAccess;
        this.supportsPerpetualAnimation = supportsPerpetualAnimation;
        this.animationDurationLimit = animationDurationLimit;
    }

    /**
     * @return Whether this animation needs write access. When true, the state of the world or the structure will be
     * affected by the animation. When false, the animation is side effect free.
     */
    public boolean requiresWriteAccess()
    {
        return requiresWriteAccess;
    }

    /**
     * @return True if animations of this type support perpetual animation.
     */
    public boolean allowsPerpetualAnimation()
    {
        return supportsPerpetualAnimation;
    }

    /**
     * @return The time limit (in seconds) for an animation of this time. This value is set to {@link Double#MAX_VALUE}
     * for any type that has no limit.
     */
    public double getAnimationDurationLimit()
    {
        return animationDurationLimit;
    }
}
