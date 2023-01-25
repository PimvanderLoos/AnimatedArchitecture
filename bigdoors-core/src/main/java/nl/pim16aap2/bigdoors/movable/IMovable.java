package nl.pim16aap2.bigdoors.movable;

import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

/**
 * Represents a movable.
 *
 * @author Pim
 */
public interface IMovable extends IMovableConst
{
    /**
     * Changes the open-status of this movable. True if open, False if closed.
     *
     * @param bool
     *     The new open-status of the movable.
     */
    void setOpen(boolean bool);

    /**
     * Changes the lock status of this movable. Locked movables cannot be opened.
     *
     * @param locked
     *     New lock status.
     */
    void setLocked(boolean locked);

    /**
     * Changes the name of the movable.
     *
     * @param name
     *     The new name of this movable.
     */
    void setName(String name);

    /**
     * Sets the {@link MovementDirection} this {@link IMovable} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction.
     *
     * @param openDirection
     *     The {@link MovementDirection} this {@link IMovable} will open in.
     */
    void setOpenDir(MovementDirection openDirection);

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
     * Changes the position of this {@link IMovable}. The min/max order of the positions does not matter.
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
     * Changes the position of this {@link IMovable}. The min/max order of the positions doesn't matter.
     *
     * @param newCuboid
     *     The {@link Cuboid} representing the area the movable will take up from now on.
     */
    void setCoordinates(Cuboid newCuboid);

    @Override
    boolean equals(Object o);
}
