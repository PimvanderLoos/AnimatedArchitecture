package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPPlayer;

/**
 * Represents several types of animation.
 */
public enum AnimationType
{
    /**
     * Animates the movement of blocks from their starting position to their final position, which may be somewhere
     * else.
     */
    MOVE_BLOCKS(true, true, Double.MAX_VALUE, true),

    /**
     * Animates a preview of an animation. No blocks are affected in the world.
     * <p>
     * Note that this type requires an online {@link IPPlayer} to target.
     */
    PREVIEW(false, false, 3, false),
    ;

    private final boolean affectsWorld;
    private final boolean supportsPerpetualAnimation;
    private final double animationDurationLimit;
    private final boolean isExclusive;

    AnimationType(
        boolean affectsWorld, boolean supportsPerpetualAnimation, double animationDurationLimit, boolean isExclusive)
    {
        this.affectsWorld = affectsWorld;
        this.supportsPerpetualAnimation = supportsPerpetualAnimation;
        this.animationDurationLimit = animationDurationLimit;
        this.isExclusive = isExclusive;
    }

    /**
     * @return Whether this animation affects the world. When true, the coordinates of the movable and the 'isOpen'
     * status will be updated once the animation has finished.
     */
    public boolean affectsWorld()
    {
        return affectsWorld;
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

    /**
     * @return True if this is an exclusive type of animation.
     * <p>
     * Exclusive here means that while an animation of this type is active, no other animation can be activated.
     * <p>
     * When false, more than one non-exclusive type of animation can be created.
     */
    public boolean isExclusive()
    {
        return isExclusive;
    }
}
