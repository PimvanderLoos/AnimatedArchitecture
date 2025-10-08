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
     * Returns whether this animated block is alive.
     *
     * @return {@code true} if this animated block is alive.
     */
    boolean isAlive();

    /**
     * Returns the block data of this animated block.
     *
     * @return The block data of this animated block.
     */
    IAnimatedBlockData getAnimatedBlockData();

    /**
     * Returns the current position of this animated block.
     * <p>
     * This value is updated after each tick/teleport.
     *
     * @return The current position of this animated block.
     */
    Vector3Dd getCurrentPosition();

    /**
     * Gets the previous position of this animated block.
     * <p>
     * This value is updated after each tick/teleport and describes the location this animated block was at before it
     * moved.
     *
     * @return The previous position of this animated block.
     */
    Vector3Dd getPreviousPosition();

    /**
     * Gets the previous movement target.
     * <p>
     * This value is updated after each tick/teleport and describes the target this animated block was moving towards
     * before it moved.
     *
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
     * <p>
     * The movement will be applied during the next tick.
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
     * Returns the number of ticks this animated block has existed for.
     *
     * @return The number of ticks this animated block has existed for.
     */
    int getTicksLived();

    /**
     * Returns the current location of this entity.
     * <p>
     * Note that this will not reflect the new location after the animated block is teleported until after the next
     * tick.
     *
     * @return The current location of this entity.
     */
    ILocation getLocation();

    /**
     * Returns the current position of this entity.
     * <p>
     * Note that this will not reflect the new position after the animated block is teleported until after the next
     * tick.
     *
     * @return The current position of this entity.
     */
    Vector3Dd getPosition();

    /**
     * Returns the starting position of this animated block (when it was first spawned).
     *
     * @return The starting position of this animated block.
     */
    RotatedPosition getStartPosition();

    /**
     * Returns the position where the block will be placed after the animation finishes.
     *
     * @return The position where the block will be placed after the animation finishes.
     */
    RotatedPosition getFinalPosition();

    /**
     * Returns the x-coordinate of the location the block was first spawned at.
     *
     * @return The x-coordinate of the location the block was first spawned at.
     */
    default double getStartX()
    {
        return getStartPosition().position().x();
    }

    /**
     * Returns the y-coordinate of the location the block was first spawned at.
     *
     * @return The y-coordinate of the location the block was first spawned at.
     */
    default double getStartY()
    {
        return getStartPosition().position().y();
    }

    /**
     * Returns the z-coordinate of the location the block was first spawned at.
     *
     * @return The z-coordinate of the location the block was first spawned at.
     */
    default double getStartZ()
    {
        return getStartPosition().position().z();
    }

    /**
     * Returns the radius this animated block had in relation to the engine when the animation first started.
     *
     * @return The radius this animated block had in relation to the engine when the animation first started.
     */
    float getRadius();

    /**
     * The different modes for teleporting an animated block.
     */
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
