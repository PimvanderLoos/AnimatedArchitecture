package nl.pim16aap2.bigdoors.core.api;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.pim16aap2.bigdoors.core.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;

/**
 * Represents a position in a world.
 * <p>
 * Note that, unlike the Spigot Location, this location is not mutable!
 *
 * @author Pim
 */
public interface ILocation
{
    /**
     * Changes the x coordinate.
     *
     * @param newVal
     *     The new coordinate.
     * @return A new {@link ILocation}.
     */
    @CheckReturnValue @Contract(pure = true)
    ILocation setX(double newVal);

    /**
     * Changes the y coordinate.
     *
     * @param newVal
     *     The new coordinate.
     * @return A new {@link ILocation}.
     */
    @CheckReturnValue @Contract(pure = true)
    ILocation setY(double newVal);

    /**
     * Changes the z coordinate.@CheckReturnValue
     *
     * @param newVal
     *     The new coordinate.
     * @return A new {@link ILocation}.
     */
    @CheckReturnValue @Contract(pure = true)
    ILocation setZ(double newVal);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param x
     *     The value to add to the x coordinate.
     * @param y
     *     The value to add to the y coordinate.
     * @param z
     *     The value to add to the z coordinate.
     * @return A new {@link ILocation}.
     */
    @CheckReturnValue @Contract(pure = true)
    ILocation add(double x, double y, double z);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector
     *     The vector to add to the coordinates.
     * @return A new {@link ILocation}.
     */
    @CheckReturnValue @Contract(pure = true)
    ILocation add(Vector3Di vector);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector
     *     The vector to add to the coordinates.
     * @return A new {@link ILocation}.
     */
    @CheckReturnValue @Contract(pure = true)
    ILocation add(Vector3Dd vector);

    /**
     * Gets the world of this location.
     *
     * @return The world of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    IWorld getWorld();

    /**
     * Gets the chunk coordinates of the chunk this location is in.
     *
     * @return The chunk coordinates of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    Vector2Di getChunk();

    /**
     * Gets the X value of this location.
     *
     * @return The X value of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    int getBlockX();

    /**
     * Gets the Y value of this location.
     *
     * @return The Y value of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    int getBlockY();

    /**
     * Gets the Z value of this location.
     *
     * @return The Z value of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    int getBlockZ();

    /**
     * Gets the X value of this location.
     *
     * @return The X value of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    double getX();

    /**
     * Gets the Y value of this location.
     *
     * @return The Y value of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    double getY();

    /**
     * Gets the Z value of this location.
     *
     * @return The Z value of this location.
     */
    @CheckReturnValue @Contract(pure = true)
    double getZ();

    @CheckReturnValue @Contract(pure = true)
    default Vector3Di getPosition()
    {
        return new Vector3Di(getBlockX(), getBlockY(), getBlockZ());
    }

    @CheckReturnValue @Contract(pure = true)
    default Vector3Dd getPositionDouble()
    {
        return new Vector3Dd(getX(), getY(), getZ());
    }

    /**
     * Gets the position (so no world) in integers as a String.
     *
     * @return The position in integers as a String.
     */
    @CheckReturnValue @Contract(pure = true)
    default String toIntPositionString()
    {
        return String.format("(%d;%d;%d)", getBlockX(), getBlockY(), getBlockZ());
    }

    /**
     * Gets the position (so no world) in double as a String.
     *
     * @return The position in double as a String.
     */
    @CheckReturnValue @Contract(pure = true)
    default String toDoublePositionString()
    {
        return String.format("(%.2f;%.2f;%.2f)", getX(), getY(), getZ());
    }
}
