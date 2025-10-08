package nl.pim16aap2.animatedarchitecture.core.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import org.jetbrains.annotations.Contract;

/**
 * A 3d vector backed either by doubles or integers.
 */
public sealed interface IVector3D permits Vector3Dd, Vector3Di
{
    /**
     * Returns the x value as double.
     *
     * @return The x value as double.
     */
    @CheckReturnValue
    @Contract(pure = true)
    double xD();

    /**
     * Returns the y value as double.
     *
     * @return The y value as double.
     */
    @CheckReturnValue
    @Contract(pure = true)
    double yD();

    /**
     * Returns the z value as double.
     *
     * @return The z value as double.
     */
    @CheckReturnValue
    @Contract(pure = true)
    double zD();

    /**
     * Gets the distance to a point.
     *
     * @param point
     *     The point.
     * @return The distance to the other point.
     */
    @CheckReturnValue
    @Contract(pure = true)
    default double getDistance(IVector3D point)
    {
        return Vector3DUtil.getDistance(this, point);
    }

    /**
     * Gets the distance to a location.
     *
     * @param location
     *     The location.
     * @return The distance to the other point.
     */
    @CheckReturnValue
    @Contract(pure = true)
    default double getDistance(ILocation location)
    {
        return Vector3DUtil.getDistance(
            this.xD(), this.yD(), this.zD(),
            location.getX(), location.getY(), location.getZ());
    }

    /**
     * Creates a new {@link ILocation} using the current x/y/z coordinates.
     *
     * @param world
     *     The world in which the {@link ILocation} will exist.
     * @return A new {@link ILocation}.
     */
    @CheckReturnValue
    @Contract(pure = true)
    default ILocation toLocation(ILocationFactory locationFactory, IWorld world)
    {
        return locationFactory.create(world, xD(), yD(), zD());
    }

    /**
     * Returns the magnitude of this vector.
     *
     * @return The magnitude of this vector.
     */
    @CheckReturnValue
    @Contract(pure = true)
    default double magnitude()
    {
        return Math.sqrt(Math.pow(xD(), 2) + Math.pow(yD(), 2) + Math.pow(zD(), 2));
    }

    /**
     * Returns a new vector with {@link Math#floor(double)} applied to the current values.
     *
     * @return A new vector with {@link Math#floor(double)} applied to the current values.
     */
    IVector3D floor();

    /**
     * Returns a new vector with {@link Math#round(double)} applied to the current values.
     *
     * @return A new vector with {@link Math#round(double)} applied to the current values.
     */
    IVector3D round();

    /**
     * Returns a new vector with {@link Math#ceil(double)} applied to the current values.
     *
     * @return A new vector with {@link Math#ceil(double)} applied to the current values.
     */
    IVector3D ceil();

    /**
     * Converts all the values in this 3d vector to tadians.
     * <p>
     * See {@link Math#toRadians(double)}.
     *
     * @return The new 3d vector.
     */
    default Vector3Dd toRadians()
    {
        return new Vector3Dd(Math.toRadians(xD()), Math.toRadians(yD()), Math.toRadians(zD()));
    }

    /**
     * Converts all the values in this 3d vector to degrees.
     * <p>
     * See {@link Math#toDegrees(double)}.
     *
     * @return The new 3d vector.
     */
    default Vector3Dd toDegrees()
    {
        return new Vector3Dd(Math.toDegrees(xD()), Math.toDegrees(yD()), Math.toDegrees(zD()));
    }

    /**
     * Creates a new integer-based 3d vector from this 3d vector.
     * <p>
     * The values of a double-based vector will be rounded to obtain the integer values.
     *
     * @return A new integer-based vector.
     */
    @CheckReturnValue
    @Contract(pure = true)
    Vector3Di toInteger();

    /**
     * Creates a new double-based 3d vector from this integer-based 3d vector.
     *
     * @return A new double-based vector.
     */
    @CheckReturnValue
    @Contract(pure = true)
    Vector3Dd toDouble();
}
