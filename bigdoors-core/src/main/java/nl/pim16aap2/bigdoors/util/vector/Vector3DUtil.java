package nl.pim16aap2.bigdoors.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a set of common 3d vector methods shared by both the integer and double implementations.
 *
 * @author Pim
 */
class Vector3DUtil
{
    private Vector3DUtil()
    {
        // utility class
    }

    @CheckReturnValue @Contract(pure = true)
    static double getDistance(double x0, double y0, double z0, double x1, double y1, double z1)
    {
        return Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2) + Math.pow(z0 - z1, 2));
    }

    @CheckReturnValue @Contract(pure = true)
    static double getDistance(@NotNull IVector3D from, @NotNull IVector3D to)
    {
        return getDistance(from.xD(), from.yD(), from.zD(), to.xD(), to.yD(), to.zD());
    }

    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    static @NotNull Vector3Dd rotateAroundXAxis(@NotNull IVector3D basePoint, @NotNull IVector3D pivotPoint,
                                                double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedY = basePoint.yD() - pivotPoint.yD();
        final double translatedZ = basePoint.zD() - pivotPoint.zD();

        final double changeY = cos * translatedY - sin * translatedZ;
        final double changeZ = sin * translatedY + cos * translatedZ;

        final double newY = pivotPoint.yD() + changeY;
        final double newZ = pivotPoint.zD() + changeZ;

        return new Vector3Dd(basePoint.xD(), newY, newZ);
    }

    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    static @NotNull Vector3Dd rotateAroundYAxis(@NotNull IVector3D basePoint, @NotNull IVector3D pivotPoint,
                                                double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedX = basePoint.xD() - pivotPoint.xD();
        final double translatedZ = basePoint.zD() - pivotPoint.zD();

        final double changeX = cos * translatedX - sin * translatedZ;
        final double changeZ = sin * translatedX + cos * translatedZ;

        final double newX = pivotPoint.xD() + changeX;
        final double newZ = pivotPoint.zD() + changeZ;
        return new Vector3Dd(newX, basePoint.yD(), newZ);
    }

    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    static @NotNull Vector3Dd rotateAroundZAxis(@NotNull IVector3D basePoint, @NotNull IVector3D pivotPoint,
                                                double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedX = basePoint.xD() - pivotPoint.xD();
        final double translatedY = basePoint.yD() - pivotPoint.yD();

        final double changeX = sin * translatedY + cos * translatedX;
        final double changeY = cos * translatedY - sin * translatedX;

        final double newX = pivotPoint.xD() + changeX;
        final double newY = pivotPoint.yD() + changeY;

        return new Vector3Dd(newX, newY, basePoint.zD());
    }

    sealed interface IVector3D permits Vector3Dd, Vector3Di
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
         * @param point The point.
         * @return The distance to the other point.
         */
        @CheckReturnValue
        @Contract(pure = true)
        default double getDistance(@NotNull IVector3D point)
        {
            return Vector3DUtil.getDistance(this, point);
        }

        /**
         * Creates a new {@link IPLocation} using the current x/y/z coordinates.
         *
         * @param world The world in which the {@link IPLocation} will exist.
         * @return A new {@link IPLocation}.
         */
        @CheckReturnValue
        @Contract(value = "_ -> new", pure = true)
        default @NotNull IPLocation toLocation(@NotNull IPWorld world)
        {
            return BigDoors.get().getPlatform().getPLocationFactory().create(world, xD(), yD(), zD());
        }
    }
}
