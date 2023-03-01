package nl.pim16aap2.animatedarchitecture.core.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

/**
 * Represents a set of common 3d vector methods shared by both the integer and double implementations.
 *
 * @author Pim
 */
final class Vector3DUtil
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
    static double getDistance(IVector3D from, IVector3D to)
    {
        return getDistance(from.xD(), from.yD(), from.zD(), to.xD(), to.yD(), to.zD());
    }

    @CheckReturnValue @Contract(pure = true)
    static Vector3Dd rotateAroundXAxis(IVector3D basePoint, IVector3D pivotPoint, double radians)
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

    @CheckReturnValue @Contract(pure = true)
    static Vector3Dd rotateAroundYAxis(IVector3D basePoint, IVector3D pivotPoint, double radians)
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

    @CheckReturnValue @Contract(pure = true)
    static Vector3Dd rotateAroundZAxis(IVector3D basePoint, IVector3D pivotPoint, double radians)
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
}
