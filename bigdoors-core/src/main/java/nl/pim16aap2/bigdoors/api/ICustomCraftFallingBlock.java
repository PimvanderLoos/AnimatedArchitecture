package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a customized version of the CraftBukkitFallingBlock.
 *
 * @author Pim
 */
public interface ICustomCraftFallingBlock
{
    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition  The location that the entity will be reported to.
     * @param rotation     The local rotations of the entity.
     * @param teleportMode How to handle the teleport.
     * @return True if the teleport was successful.
     */
    boolean teleport(@NotNull Vector3Dd newPosition, @NotNull Vector3Dd rotation,
                     @NotNull TeleportMode teleportMode);

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition The location that the entity will be reported to.
     * @param rotation    The local rotations of the entity.
     * @return True if the teleport was successful.
     */
    default boolean teleport(@NotNull Vector3Dd newPosition, @NotNull Vector3Dd rotation)
    {
        return teleport(newPosition, rotation, TeleportMode.SET_VELOCITY);
    }

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    default boolean teleport(@NotNull Vector3Dd newPosition)
    {
        return teleport(newPosition, new Vector3Dd(0, 0, 0), TeleportMode.SET_VELOCITY);
    }

    /**
     * Removes the entity from the world.
     */
    void remove();

    /**
     * Gets the current location of this entity.
     *
     * @return The current location of this entity.
     */
    @NotNull IPLocation getPLocation();

    /**
     * Gets the current position of this entity.
     *
     * @return The current position of this entity.
     */
    @NotNull Vector3Dd getPosition();

    /**
     * Gets the current velocity of this entity.
     *
     * @return The current velocity of this entity.
     */
    @NotNull Vector3Dd getPVelocity();

    /**
     * Sets the velocity of the entity.
     *
     * @param vector The new velocity of the entity.
     */
    void setVelocity(@NotNull Vector3Dd vector);

    /**
     * Sets the headPose of this entity.
     *
     * @param pose The new pose of this entity's head.
     */
    void setHeadPose(@NotNull Vector3Dd pose);

    /**
     * Sets the headPose of this entity.
     *
     * @param eulerAngle The new pose of this entity's head described as a EulerAngle.
     */
    void setBodyPose(@NotNull Vector3Dd eulerAngle);

    enum TeleportMode
    {
        SET_VELOCITY,
        NO_VELOCITY
    }
}
