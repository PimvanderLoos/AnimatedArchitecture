package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a mutable position in a world.
 *
 * @author Pim
 */
public interface IPLocation extends IPLocationConst, Cloneable
{
    /** {@inheritDoc} */
    @Override
    @NotNull
    IPWorld getWorld();

    /**
     * Updates the world of this location.
     *
     * @param other The new world of this location.
     */
    void setWorld(final @NotNull IPWorld other);

    /** {@inheritDoc} */
    @Override
    @NotNull
    Vector2Di getChunk();

    /** {@inheritDoc} */
    @Override
    int getBlockX();

    /** {@inheritDoc} */
    @Override
    int getBlockY();

    /** {@inheritDoc} */
    @Override
    int getBlockZ();

    /** {@inheritDoc} */
    @Override
    double getX();

    /** {@inheritDoc} */
    @Override
    double getY();

    /** {@inheritDoc} */
    @Override
    double getZ();

    /** {@inheritDoc} */
    void setX(double newVal);

    /**
     * Changes the y coordinate.
     *
     * @param newVal The new coordinate.
     */
    void setY(double newVal);

    /**
     * Changes the z coordinate.
     *
     * @param newVal The new coordinate.
     */
    void setZ(double newVal);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param x The value to add to the x coordinate.
     * @param y The value to add to the y coordinate.
     * @param z The value to add to the z coordinate.
     * @return This current IPLocation.
     */
    @NotNull
    IPLocation add(final double x, final double y, final double z);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return This current IPLocation.
     */
    @NotNull
    IPLocation add(final @NotNull IVector3DiConst vector);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return This current IPLocation.
     */
    @NotNull
    IPLocation add(final @NotNull IVector3DdConst vector);

    /** {@inheritDoc} */
    @NotNull
    IPLocation clone();
}
