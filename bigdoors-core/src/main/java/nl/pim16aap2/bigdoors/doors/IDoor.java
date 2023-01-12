package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

/**
 * Represents a door.
 *
 * @author Pim
 */
public interface IDoor extends IDoorConst
{
    /**
     * Changes the open-status of this door. True if open, False if closed.
     *
     * @param bool
     *     The new open-status of the door.
     */
    void setOpen(boolean bool);

    /**
     * Changes the lock status of this door. Locked doors cannot be opened.
     *
     * @param locked
     *     New lock status.
     */
    void setLocked(boolean locked);

    /**
     * Changes the name of the door.
     *
     * @param name
     *     The new name of this door.
     */
    void setName(String name);

    /**
     * Sets the {@link RotateDirection} this {@link IDoor} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction.
     *
     * @param rotateDirection
     *     The {@link RotateDirection} this {@link IDoor} will open in.
     */
    void setOpenDir(RotateDirection rotateDirection);

    /**
     * Updates the position of the powerblock.
     *
     * @param pos
     *     The new position.
     */
    void setPowerBlock(Vector3Di pos);

    /**
     * Updates the position of the rotation point.
     *
     * @param pos
     *     The new position.
     */
    void setRotationPoint(Vector3Di pos);

    /**
     * Changes the position of this {@link IDoor}. The min/max order of the positions does not matter.
     *
     * @param posA
     *     The first new position.
     * @param posB
     *     The second new position.
     */
    default void setCoordinates(Vector3Di posA, Vector3Di posB)
    {
        setCoordinates(new Cuboid(posA, posB));
    }

    /**
     * Changes the position of this {@link IDoor}. The min/max order of the positions doesn't matter.
     *
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up from now on.
     */
    void setCoordinates(Cuboid newCuboid);

    @Override
    boolean equals(Object o);
}
