package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
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
     * @param newPos The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    boolean teleport(final @NotNull Location newPos);

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
    Location getLocation();

    /**
     * Gets the current velocity of this entity.
     *
     * @return The current velocity of this entity.
     */
    @NotNull
    Vector getVelocity();

    /**
     * Sets the velocity of the entity.
     *
     * @param vector The new velocity of the entity.
     */
    void setVelocity(final @NotNull Vector vector);

    /**
     * Sets the headPose of this entity.
     *
     * @param pose The new pose of this entity's head.
     */
    void setHeadPose(final @NotNull EulerAngle pose);

    /**
     * Sets the headPose of this entity.
     *
     * @param eulerAngle The new pose of this entity's head described as a EulerAngle.
     */
    void setBodyPose(final @NotNull EulerAngle eulerAngle);
}
