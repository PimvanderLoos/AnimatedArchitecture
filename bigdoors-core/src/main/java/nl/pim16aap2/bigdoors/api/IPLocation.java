package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a mutable position in a world.
 *
 * @author Pim
 */
public interface IPLocation extends IPLocationConst, Cloneable
{
    /**
     * Changes the x coordinate.
     *
     * @param newVal The new coordinate.
     */
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
    @NotNull IPLocation add(double x, double y, double z);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return This current IPLocation.
     */
    @NotNull IPLocation add(@NotNull Vector3Di vector);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return This current IPLocation.
     */
    @NotNull IPLocation add(@NotNull Vector3Dd vector);

    @NotNull IPLocation clone();
}
