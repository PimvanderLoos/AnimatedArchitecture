package nl.pim16aap2.bigdoors.core.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.api.factories.IPLocationFactory;
import org.jetbrains.annotations.Contract;

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
     * Creates a new {@link IPLocation} using the current x/y/z coordinates.
     *
     * @param world
     *     The world in which the {@link IPLocation} will exist.
     * @return A new {@link IPLocation}.
     */
    @CheckReturnValue @Contract(pure = true)
    default IPLocation toLocation(IPLocationFactory locationFactory, IPWorld world)
    {
        return locationFactory.create(world, xD(), yD(), zD());
    }

    /**
     * @return The magnitude of this vector.
     */
    @CheckReturnValue @Contract(pure = true)
    default double magnitude()
    {
        return Math.sqrt(Math.pow(xD(), 2) + Math.pow(yD(), 2) + Math.pow(zD(), 2));
    }

    /**
     * @return A new vector with {@link Math#floor(double)} applied to the current values.
     */
    IVector3D floor();
}
