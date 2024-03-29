package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

/**
 * Represents a block that is being animated.
 */
@SuppressWarnings("unused")
public interface IAnimatedBlock
{
    /**
     * @return True if this animated block is alive.
     */
    boolean isAlive();

    /**
     * @return The block data of this animated block.
     */
    IAnimatedBlockData getAnimatedBlockData();

    /**
     * @return The current position of this animated block. This value is updated after each tick/teleport.
     */
    Vector3Dd getCurrentPosition();

    /**
     * Gets the previous position of this animated block. This value is updated after each tick/teleport and describes
     * the location this animated block was at before it moved.
     *
     * @return The previous position of this animated block.
     */
    Vector3Dd getPreviousPosition();

    /**
     * @return The previous movement target.
     */
    Vector3Dd getPreviousTarget();

    /**
     * Gets the world this animated block exists in.
     *
     * @return The world this animated block exists in.
     */
    IWorld getWorld();

    /**
     * Moves this animated block to the target.
     *
     * @param target
     *     The target position the block should move to.
     */
    void moveToTarget(RotatedPosition target);

    /**
     * Spawns this animated block.
     */
    void spawn();

    /**
     * Respawns this animated block.
     */
    void respawn();

    /**
     * Removes the entity from the world.
     */
    void kill();

    /**
     * @return The number of ticks this animated block has existed for.
     */
    int getTicksLived();

    /**
     * @return The current location of this entity.
     * <p>
     * Note that this will not reflect the new location after the animated block is teleported until after the next
     * tick.
     */
    ILocation getLocation();

    /**
     * @return The current position of this entity.
     * <p>
     * Note that this will not reflect the new position after the animated block is teleported until after the next
     * tick.
     */
    Vector3Dd getPosition();

    /**
     * @return The starting position of this animated block.
     */
    RotatedPosition getStartPosition();

    /**
     * @return The position where the block will be placed after the animation finishes.
     */
    RotatedPosition getFinalPosition();

    /**
     * @return The x-coordinate of the location the block was first spawned at.
     */
    default double getStartX()
    {
        return getStartPosition().position().x();
    }

    /**
     * @return The y-coordinate of the location the block was first spawned at.
     */
    default double getStartY()
    {
        return getStartPosition().position().y();
    }

    /**
     * @return The z-coordinate of the location the block was first spawned at.
     */
    default double getStartZ()
    {
        return getStartPosition().position().z();
    }

    /**
     * @return The radius this animated block had in relation to the engine when the animation first started.
     */
    float getRadius();

    enum TeleportMode
    {
        /**
         * Teleports the animated object relative to its old location.
         */
        RELATIVE,

        /**
         * Teleports the animated object to the absolute location.
         */
        ABSOLUTE
    }
}
