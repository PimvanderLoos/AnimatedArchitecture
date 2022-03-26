package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a customized version of the EntityFallingBlock from NMS.
 *
 * @author Pim
 */
public interface IAnimatedBlock
{
    /**
     * Checks if this animated block is currently alive.
     *
     * @return True if this animated block is alive.
     */
    boolean isAlive();

    /**
     * Gets the current position of this animated block. This value is updated after each tick/teleport.
     *
     * @return The current position of this animated block.
     */
    Vector3Dd getCurrentPosition();

    /**
     * Gets the previous position of this animated block. This value is updated after each tick/teleport and descibes
     * the location this animated block was at before it moved.
     *
     * @return The previous position of this animated block.
     */
    Vector3Dd getPreviousPosition();

    /**
     * Gets the current velocity in x/y/z terms for this animated block.
     *
     * @return The current velocity of this animated block.
     */
    Vector3Dd getVelocity();

    /**
     * Gets the world this animated block exists in.
     *
     * @return The world this animated block exists in.
     */
    IPWorld getPWorld();

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition
     *     The location that the entity will be reported to.
     * @param rotation
     *     The local rotations of the entity.
     * @param teleportMode
     *     How to handle the teleport.
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
        return teleport(newPosition, rotation, TeleportMode.SET_VELOCITY);
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
        return teleport(newPosition, new Vector3Dd(0, 0, 0), TeleportMode.SET_VELOCITY);
    }

    /**
     * Removes the entity from the world.
     */
    void kill();

    /**
     * Gets the current location of this entity.
     *
     * @return The current location of this entity.
     */
    IPLocation getPLocation();

    /**
     * Gets the current position of this entity.
     *
     * @return The current position of this entity.
     */
    Vector3Dd getPosition();

    /**
     * Gets the current velocity of this entity.
     *
     * @return The current velocity of this entity.
     */
    Vector3Dd getPVelocity();

    /**
     * Sets the velocity of the entity.
     *
     * @param vector
     *     The new velocity of the entity.
     */
    void setVelocity(Vector3Dd vector);

    enum TeleportMode
    {
        SET_VELOCITY,
        NO_VELOCITY
    }
}
