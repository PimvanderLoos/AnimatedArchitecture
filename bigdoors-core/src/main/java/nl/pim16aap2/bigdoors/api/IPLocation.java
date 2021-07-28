package nl.pim16aap2.bigdoors.api;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a position in a world.
 * <p>
 * Note that, unlike the Spigot Location, this location is not mutable!
 *
 * @author Pim
 */
public interface IPLocation
{
    /**
     * Changes the x coordinate.
     *
     * @param newVal The new coordinate.
     * @return A new {@link IPLocation}.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    @NotNull IPLocation setX(double newVal);

    /**
     * Changes the y coordinate.
     *
     * @param newVal The new coordinate.
     * @return A new {@link IPLocation}.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    @NotNull IPLocation setY(double newVal);

    /**
     * Changes the z coordinate.@CheckReturnValue
     *
     * @param newVal The new coordinate.
     * @return A new {@link IPLocation}.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    @NotNull IPLocation setZ(double newVal);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param x The value to add to the x coordinate.
     * @param y The value to add to the y coordinate.
     * @param z The value to add to the z coordinate.
     * @return A new {@link IPLocation}.
     */
    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    @NotNull IPLocation add(double x, double y, double z);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return A new {@link IPLocation}.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    @NotNull IPLocation add(@NotNull Vector3Di vector);

    /**
     * Adds values to the coordinates of this location.
     *
     * @param vector The vector to add to the coordinates.
     * @return A new {@link IPLocation}.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    @NotNull IPLocation add(@NotNull Vector3Dd vector);

    /**
     * Gets the world of this location.
     *
     * @return The world of this location.
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
    @NotNull IPWorld getWorld();

    /**
     * Gets the chunk coordinates of the chunk this location is in.
     *
     * @return The chunk coordinates of this location.
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
    @NotNull Vector2Di getChunk();

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

    @CheckReturnValue @Contract(value = " -> new", pure = true)
    default @NotNull Vector3Di getPosition()
    {
        return new Vector3Di(getBlockX(), getBlockY(), getBlockZ());
    }

    /**
     * Gets the position (so no world) in integers as a String.
     *
     * @return The position in integers as a String.
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
    default @NotNull String toIntPositionString()
    {
        return String.format("(%d;%d;%d)", getBlockX(), getBlockY(), getBlockZ());
    }

    /**
     * Gets the position (so no world) in double as a String.
     *
     * @return The position in double as a String.
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
    default @NotNull String toDoublePositionString()
    {
        return String.format("(%.2f;%.2f;%.2f)", getX(), getY(), getZ());
    }
}
