package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
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
    boolean teleport(final @NotNull Vector3DdConst newPosition, final @NotNull Vector3DdConst rotation,
                     final @NotNull TeleportMode teleportMode);

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition The location that the entity will be reported to.
     * @param rotation    The local rotations of the entity.
     * @return True if the teleport was successful.
     */
    default boolean teleport(final @NotNull Vector3DdConst newPosition, final @NotNull Vector3DdConst rotation)
    {
        return teleport(newPosition, rotation, TeleportMode.SET_VELOCITY);
    }

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    default boolean teleport(final @NotNull Vector3DdConst newPosition)
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
    void setVelocity(final @NotNull Vector3DdConst vector);

    /**
     * Sets the headPose of this entity.
     *
     * @param pose The new pose of this entity's head.
     */
    void setHeadPose(final @NotNull Vector3DdConst pose);

    /**
     * Sets the headPose of this entity.
     *
     * @param eulerAngle The new pose of this entity's head described as a EulerAngle.
     */
    void setBodyPose(final @NotNull Vector3DdConst eulerAngle);

    enum TeleportMode
    {
        SET_VELOCITY,
        NO_VELOCITY
    }
}
