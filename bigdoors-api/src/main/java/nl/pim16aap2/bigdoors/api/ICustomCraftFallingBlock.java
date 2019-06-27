package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

/**
 * Represents a customized version of the CraftBukkitFallingBlock.
 *
 * @autor Pim
 */
public interface ICustomCraftFallingBlock
{
    /**
     * Teleport the entity to the provided location.
     * 
     * @param newPos The location that the entity will be reported to.
     * @return True if the teleport was successful.
     */
    boolean teleport(Location newPos);

    /**
     * Remove the entity from the world.
     */
    void remove();

    /**
     * Set the velocity of the entity.
     * 
     * @param vector The new velocity of the entity.
     */
    void setVelocity(Vector vector);

    /**
     * Get the current location of this entity.
     * 
     * @return The current location of this entity.
     */
    Location getLocation();

    /**
     * Get the current velocity of this entity.
     * 
     * @return The current velocity of this entity.
     */
    Vector getVelocity();

    /**
     * Set the headPose of this entity.
     * 
     * @param pose The new pose of this entity's head.
     */
    void setHeadPose(EulerAngle pose);

    /**
     * Set the headPose of this entity.
     * 
     * @param eulerAngle The new pose of this entity's head described as a
     *                   EulerAngle.
     */
    void setBodyPose(EulerAngle eulerAngle);
}
