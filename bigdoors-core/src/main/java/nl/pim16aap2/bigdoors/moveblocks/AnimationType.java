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
    MOVE_BLOCKS(true, true, Double.MAX_VALUE),

    /**
     * Animates a preview of an animation. No blocks are affected in the world.
     * <p>
     * Note that this type requires an online {@link IPPlayer} to target.
     */
    PREVIEW(false, false, 3),
    ;

    private final boolean affectsWorld;
    private final boolean supportsPerpetualAnimation;
    private final double animationDurationLimit;

    AnimationType(boolean affectsWorld, boolean supportsPerpetualAnimation, double animationDurationLimit)
    {
        this.affectsWorld = affectsWorld;
        this.supportsPerpetualAnimation = supportsPerpetualAnimation;
        this.animationDurationLimit = animationDurationLimit;
    }

    /**
     * @return Whether this animation affects the world. When true, the coordinates of the movable and the 'isOpen'
     * status will be updated once the animation has finished.
     */
    public boolean affectsWorld()
    {
        return affectsWorld;
    }

    public boolean allowsPerpetualAnimation()
    {
        return supportsPerpetualAnimation;
    }

    public double getAnimationDurationLimit()
    {
        return animationDurationLimit;
    }
}
