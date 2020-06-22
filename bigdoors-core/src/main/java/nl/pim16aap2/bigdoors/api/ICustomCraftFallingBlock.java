package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
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
     * Teleports the entity to the provided location.
     *
     * @param newLocation The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    boolean teleport(final @NotNull IPLocation newLocation);

    /**
     * Teleports the entity to the provided position.
     *
     * @param newPosition The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    boolean teleport(final @NotNull IVector3DdConst newPosition);

    /**
     * Removes the entity from the world.
     */
    void remove();

    /**
     * Gets the current location of this entity.
     *
     * @return The current location of this entity.
     */
    @NotNull
    IPLocation getPLocation();

    /**
     * Gets the current position of this entity.
     *
     * @return The current position of this entity.
     */
    @NotNull
    Vector3Dd getPosition();

    /**
     * Gets the current velocity of this entity.
     *
     * @return The current velocity of this entity.
     */
    @NotNull
    Vector3Dd getPVelocity();

    /**
     * Sets the velocity of the entity.
     *
     * @param vector The new velocity of the entity.
     */
    void setVelocity(final @NotNull IVector3DdConst vector);

    /**
     * Sets the headPose of this entity.
     *
     * @param pose The new pose of this entity's head.
     */
    void setHeadPose(final @NotNull IVector3DdConst pose);

    /**
     * Sets the headPose of this entity.
     *
     * @param eulerAngle The new pose of this entity's head described as a EulerAngle.
     */
    void setBodyPose(final @NotNull IVector3DdConst eulerAngle);
}
