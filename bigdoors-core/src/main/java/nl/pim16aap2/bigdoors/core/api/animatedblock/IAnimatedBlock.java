package nl.pim16aap2.bigdoors.core.api.animatedblock;

import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;

/**
 * Represents a block that is being animated.
 *
 * @author Pim
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
     *     The target position the block should move to(wards).
     * @param ticksRemaining
     */
    void moveToTarget(Vector3Dd target, int ticksRemaining);

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition
     *     The location that the entity will be reported to.
     * @param rotation
     *     The local rotations of the entity.
     * @param teleportMode
     *     The type of teleportation to apply.
     * @return True if the teleport was successful.
     */
    boolean teleport(Vector3Dd newPosition, Vector3Dd rotation, TeleportMode teleportMode);

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition
     *     The location that the entity will be reported to.
     * @param rotation
     *     The local rotations of the entity.
     * @return True if the teleport was successful.
     */
    default boolean teleport(Vector3Dd newPosition, Vector3Dd rotation)
    {
        return teleport(newPosition, new Vector3Dd(0, 0, 0), TeleportMode.RELATIVE);
    }

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition
     *     The location that the entity will be reported to.
     * @param teleportMode
     *     The type of teleportation to apply.
     * @return True if the teleport was successful.
     */
    default boolean teleport(Vector3Dd newPosition, TeleportMode teleportMode)
    {
        return teleport(newPosition, new Vector3Dd(0, 0, 0), teleportMode);
    }

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition
     *     The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    default boolean teleport(Vector3Dd newPosition)
    {
        return teleport(newPosition, new Vector3Dd(0, 0, 0));
    }

    /**
     * @param vector
     *     The new velocity of the entity.
     */
    void setVelocity(Vector3Dd vector);

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
    Vector3Dd getStartPosition();

    /**
     * @return The position where the block will be placed after the animation finishes.
     */
    Vector3Dd getFinalPosition();

    /**
     * @return The x-coordinate of the location the block was first spawned at.
     */
    double getStartX();

    /**
     * @return The y-coordinate of the location the block was first spawned at.
     */
    double getStartY();

    /**
     * @return The z-coordinate of the location the block was first spawned at.
     */
    double getStartZ();

    /**
     * @return The angle this animated block had in relation to the engine when the animation first started.
     */
    float getStartAngle();

    /**
     * @return The radius this animated block had in relation to the engine when the animation first started.
     */
    float getRadius();

    /**
     * @return True if this animated block is on the edge of the cuboid being animated.
     */
    boolean isOnEdge();

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
