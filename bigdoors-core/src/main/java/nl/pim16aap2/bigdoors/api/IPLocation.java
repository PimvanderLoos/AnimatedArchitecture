package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;

public interface IPLocation
{
    /**
     * Gets the world of this location.
     *
     * @return The world of this location.
     */
    @NotNull
    IPWorld getWorld();

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
}
