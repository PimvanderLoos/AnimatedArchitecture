package nl.pim16aap2.bigdoors.moveblocks;

/**
 * Represents several types of animation.
 */
public enum AnimationType
{
    /**
     * Animates the movement of blocks from their starting position to their final position, which may be somewhere
     * else.
     */
    MOVE_BLOCKS(true),

    /**
     * Animates a preview of an animation. No blocks are affected in the world.
     */
    PREVIEW(false),
    ;

    private final boolean affectsWorld;

    AnimationType(boolean affectsWorld)
    {
        this.affectsWorld = affectsWorld;
    }

    /**
     * @return Whether this animation affects the world. When true, the coordinates of the movable and the 'isOpen'
     * status will be updated once the animation has finished.
     */
    public boolean affectsWorld()
    {
        return affectsWorld;
    }
}
