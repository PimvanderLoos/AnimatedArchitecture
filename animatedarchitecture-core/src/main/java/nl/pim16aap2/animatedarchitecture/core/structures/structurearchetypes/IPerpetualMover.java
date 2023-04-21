package nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

/**
 * Represents structures that can move perpetually. For example, windmills and flags.
 */
public interface IPerpetualMover extends IStructureConst
{
    /**
     * Changes the open-status of this structure. True if open, False if closed.
     *
     * @param bool
     *     The new open-status of the structure.
     */
    void setOpen(boolean bool);

    /**
     * Changes the lock status of this structure. Locked structures cannot be opened.
     *
     * @param locked
     *     New lock status.
     */
    void setLocked(boolean locked);

    /**
     * Changes the name of the structure.
     *
     * @param name
     *     The new name of this structure.
     */
    void setName(String name);

    /**
     * Sets the {@link MovementDirection} this {@link AbstractStructure} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction.
     *
     * @param openDirection
     *     The {@link MovementDirection} this {@link AbstractStructure} will open in.
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
     * Changes the position of this {@link AbstractStructure}. The min/max order of the positions does not matter.
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
     * Changes the position of this {@link AbstractStructure}. The min/max order of the positions doesn't matter.
     *
     * @param newCuboid
     *     The {@link Cuboid} representing the area the structure will take up from now on.
     */
    void setCoordinates(Cuboid newCuboid);
}
