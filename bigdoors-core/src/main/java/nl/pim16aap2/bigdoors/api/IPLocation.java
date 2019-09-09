package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

public interface IPLocation extends Cloneable
{
    /**
     * Gets the world of this location.
     *
     * @return The world of this location.
     */
    @NotNull
    IPWorld getWorld();

    /**
     * Gets the chunk coordinates of the chunk this location is in.
     *
     * @return The chunk coordinates of this location.
     */
    @NotNull
    Vector2Di getChunk();

    /**
     * Gets the X value of this location.
     *
     * @return The X value of this location.
     */
    int getBlockX();

    /**
     * Gets the Y value of this location.
     *
     * @return The Y value of this location.
     */
    int getBlockY();

    /**
     * Gets the Z value of this location.
     *
     * @return The Z value of this location.
     */
    int getBlockZ();

    /**
     * Gets the X value of this location.
     *
     * @return The X value of this location.
     */
    double getX();

    /**
     * Gets the Y value of this location.
     *
     * @return The Y value of this location.
     */
    double getY();

    /**
     * Gets the Z value of this location.
     *
     * @return The Z value of this location.
     */
    double getZ();

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
    @NotNull
    IPLocation add(final double x, final double y, final double z);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return This current IPLocation.
     */
    @NotNull
    IPLocation add(final @NotNull Vector3Di vector);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return This current IPLocation.
     */
    @NotNull
    IPLocation add(final @NotNull Vector3Dd vector);

    /**
     * Gets the position (so no world) in integers as a String.
     *
     * @return The position in integers as a String.
     */
    String toIntPositionString();

    /**
     * Gets the position (so no world) in double as a String.
     *
     * @return The position in double as a String.
     */
    String toDoublePositionString();

    /**
     * {@inheritDoc}
     */
    @NotNull
    IPLocation clone();
}
