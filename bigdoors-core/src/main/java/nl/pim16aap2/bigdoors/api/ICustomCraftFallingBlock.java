package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;

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
    boolean teleport(@NonNull Vector3DdConst newPosition, @NonNull Vector3DdConst rotation,
                     @NonNull TeleportMode teleportMode);

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition The location that the entity will be reported to.
     * @param rotation    The local rotations of the entity.
     * @return True if the teleport was successful.
     */
    default boolean teleport(@NonNull Vector3DdConst newPosition, @NonNull Vector3DdConst rotation)
    {
        return teleport(newPosition, rotation, TeleportMode.SET_VELOCITY);
    }

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    default boolean teleport(@NonNull Vector3DdConst newPosition)
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
    @NonNull IPLocation getPLocation();

    /**
     * Gets the current position of this entity.
     *
     * @return The current position of this entity.
     */
    @NonNull Vector3DdConst getPosition();

    /**
     * Gets the current velocity of this entity.
     *
     * @return The current velocity of this entity.
     */
    @NonNull Vector3Dd getPVelocity();

    /**
     * Sets the velocity of the entity.
     *
     * @param vector The new velocity of the entity.
     */
    void setVelocity(@NonNull Vector3DdConst vector);

    /**
     * Sets the headPose of this entity.
     *
     * @param pose The new pose of this entity's head.
     */
    void setHeadPose(@NonNull Vector3DdConst pose);

    /**
     * Sets the headPose of this entity.
     *
     * @param eulerAngle The new pose of this entity's head described as a EulerAngle.
     */
    void setBodyPose(@NonNull Vector3DdConst eulerAngle);

    enum TeleportMode
    {
        SET_VELOCITY,
        NO_VELOCITY
    }
}
