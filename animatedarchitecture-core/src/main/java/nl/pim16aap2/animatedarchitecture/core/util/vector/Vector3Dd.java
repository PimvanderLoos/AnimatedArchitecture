package nl.pim16aap2.animatedarchitecture.core.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import org.jetbrains.annotations.Contract;

/**
 * Represents a double x/y/z set.
 * <p>
 * This class is thread-safe, as all modifications return a new instance.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public record Vector3Dd(double x, double y, double z) implements IVector3D
{
    public Vector3Dd(IVector3D other)
    {
        this(other.xD(), other.yD(), other.zD());
    }

    /**
     * @param other
     *     A {@link IVector3D} to retrieve as {@link Vector3Dd}
     * @return A {@link Vector3Dd} representing the provided {@link IVector3D}. If the provided {@link IVector3D} was
     * already of this type, the same instance is returned.
     */
    public static Vector3Dd of(IVector3D other)
    {
        return other instanceof Vector3Dd vec3d ? vec3d : new Vector3Dd(other);
    }

    /**
     * Creates a new integer-based 3d vector from this double-based 3d vector.
     * <p>
     * The values of the double-based vector will be rounded to obtain the integer values.
     *
     * @return A new integer-based vector.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Di toInteger()
    {
        return Vector3Di.fromDouble(this);
    }

    @CheckReturnValue @Contract(pure = true)
    static Vector3Dd fromInteger(Vector3Di intVec)
    {
        return new Vector3Dd(intVec.x(), intVec.y(), intVec.z());
    }

    /**
     * Adds values to the current values.
     *
     * @param x
     *     The x value to add to the current x value.
     * @param y
     *     The y value to add to the current y value.
     * @param z
     *     The z value to add to the current z value.
     * @return A new {@link Vector3Dd} with the added values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd add(double x, double y, double z)
    {
        return new Vector3Dd(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Adds another 3D vector to the current values.
     *
     * @param other
     *     The other 3D vector to add to the current one.
     * @return A new {@link Vector3Dd} with the added values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd add(IVector3D other)
    {
        return add(other.xD(), other.yD(), other.zD());
    }

    /**
     * Adds a value to the x, y, and z values of the current {@link Vector3Dd}.
     *
     * @param val
     *     The value to add to the x, y, and z values.
     * @return A new {@link Vector3Dd} with the value added to the values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd add(double val)
    {
        return add(val, val, val);
    }


    /**
     * Subtracts values from the current values.
     *
     * @param x
     *     The x value to subtract from the current x value.
     * @param y
     *     The y value to subtract from the current y value.
     * @param z
     *     The z value to subtract from the current z value.
     * @return A new {@link Vector3Dd} with the subtracted values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd subtract(double x, double y, double z)
    {
        return add(-x, -y, -z);
    }

    /**
     * Subtracts another 3D vector from both the x, y, and z values of the current {@link Vector3Dd}.
     *
     * @param other
     *     The other 3D vector to subtract from the x, y, and z values.
     * @return The new {@link Vector3Dd} with the value subtracted from the values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd subtract(IVector3D other)
    {
        return subtract(other.xD(), other.yD(), other.zD());
    }

    /**
     * Subtracts a value from both the x, y, and z values of the current {@link Vector3Dd}.
     *
     * @param val
     *     The value to subtract from both the x, y, and z values.
     * @return A new {@link Vector3Dd} with the value subtracted from the values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd subtract(double val)
    {
        return subtract(val, val, val);
    }


    /**
     * Multiplies values with the current values.
     *
     * @param x
     *     The x value to multiply with the current x value.
     * @param y
     *     The y value to multiply with the current y value.
     * @param z
     *     The z value to multiply with the current z value.
     * @return A new {@link Vector3Dd} with the multiplied values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd multiply(double x, double y, double z)
    {
        return new Vector3Dd(this.x * x, this.y * y, this.z * z);
    }

    /**
     * Multiplies another 3D vector with the x, y, and z values of the current {@link Vector3Dd}.
     *
     * @param other
     *     The other 3D vector.
     * @return The new {@link Vector3Dd} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd multiply(IVector3D other)
    {
        return multiply(other.xD(), other.yD(), other.zD());
    }

    /**
     * Multiplies a value with both the x, y, and z values of the current {@link Vector3Dd}.
     *
     * @param val
     *     The value to multiply from both the x, y, and z values.
     * @return A new {@link Vector3Dd} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd multiply(double val)
    {
        return multiply(val, val, val);
    }


    /**
     * Divides the current values with values.
     *
     * @param x
     *     The x value to use as divisor for the current x value.
     * @param y
     *     The y value to use as divisor for the current y value.
     * @param z
     *     The z value to use as divisor for the current z value.
     * @return A new {@link Vector3Dd} with the divided values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd divide(double x, double y, double z)
    {
        return new Vector3Dd(this.x / x, this.y / y, this.z / z);
    }

    /**
     * Divides the x, y, and z values of the current {@link Vector3Dd} with the x, y, and z values of the provided 3D
     * vector.
     *
     * @param other
     *     The other 3D vector to use as divisor for the current x, y, and z values.
     * @return A new {@link Vector3Dd} with the divided values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd divide(IVector3D other)
    {
        return divide(other.xD(), other.yD(), other.zD());
    }

    /**
     * Divides both the x, y, and z values of the current {@link Vector3Dd} with a provided value.
     *
     * @param val
     *     The value to use as divisor for both the x, y, and z values.
     * @return A new {@link Vector3Dd} with the divided values.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd divide(double val)
    {
        return divide(val, val, val);
    }


    /**
     * Normalizes this {@link Vector3Dd}.
     *
     * @return A new {@link Vector3Dd} with normalized
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd normalized()
    {
        final double length = Math.sqrt(x * x + y * y + z * z);

        final double newX = x / length;
        final double newY = y / length;
        final double newZ = z / length;

        return new Vector3Dd(newX, newY, newZ);
    }

    /**
     * Returns a String representation of this 3d vector. The output will be the same as {@link #toString()} with the
     * only difference being that the x/y/z coordinate values will be formatted to a specific number of decimal places.
     *
     * @param decimals
     *     The number of decimals to print for each value.
     * @return A String representation of this vector.
     */
    @CheckReturnValue @Contract(pure = true)
    public String toString(int decimals)
    {
        final String placeholder = "%." + decimals + "f";
        return String.format("Vector3Dd[x=" + placeholder + ", y=" + placeholder + ", z=" + placeholder + "]", x, y, z);
    }

    /**
     * Rotates this point around another point along the x axis.
     *
     * @param pivotPoint
     *     The point around which to rotate this point.
     * @param radians
     *     How far to rotate this point (in radians).
     * @return A new {@link Vector3Dd} rotated around the pivot point.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd rotateAroundXAxis(IVector3D pivotPoint, double radians)
    {
        return Vector3DUtil.rotateAroundXAxis(this, pivotPoint, radians);
    }

    /**
     * Rotates this point around another point along the y axis.
     *
     * @param pivotPoint
     *     The point around which to rotate this point.
     * @param radians
     *     How far to rotate this point (in radians).
     * @return A new {@link Vector3Dd} rotated around the pivot point.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd rotateAroundYAxis(IVector3D pivotPoint, double radians)
    {
        return Vector3DUtil.rotateAroundYAxis(this, pivotPoint, radians);
    }

    /**
     * Rotates this point around another point along the z axis.
     *
     * @param pivotPoint
     *     The point around which to rotate this point.
     * @param radians
     *     How far to rotate this point (in radians).
     * @return A new {@link Vector3Dd} rotated around the pivot point.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd rotateAroundZAxis(IVector3D pivotPoint, double radians)
    {
        return Vector3DUtil.rotateAroundZAxis(this, pivotPoint, radians);
    }

    /**
     * @return The highest value in this vector.
     */
    @CheckReturnValue @Contract(pure = true)
    public double getMax()
    {
        return Math.max(x, Math.max(y, z));
    }

    /**
     * @return The lowest value in this vector.
     */
    @CheckReturnValue @Contract(pure = true)
    public double getMin()
    {
        return Math.min(x, Math.min(y, z));
    }

    @Override
    public double xD()
    {
        return x;
    }

    @Override
    public double yD()
    {
        return y;
    }

    @Override
    public double zD()
    {
        return z;
    }

    @Override
    public Vector3Dd floor()
    {
        return new Vector3Dd(Math.floor(x), Math.floor(y), Math.floor(z));
    }

    /**
     * Clamps the vector using a cuboid to provide the upper and lower limits.
     *
     * @param cuboid
     *     A cuboid describing the upper and lower limits of the vector.
     * @return A new vector.
     */
    public Vector3Dd clamp(Cuboid cuboid)
    {
        return clamp(cuboid.getMin(), cuboid.getMax());
    }

    /**
     * Clamps the vector to the provided upper and lower limits.
     *
     * @param min
     *     A 3d vector describing the lower limits of the vector.
     * @param max
     *     A 3d vector describing the upper limits of the vector.
     * @return A new vector.
     */
    public Vector3Dd clamp(IVector3D min, IVector3D max)
    {
        return new Vector3Dd(
            MathUtil.clamp(x, min.xD(), max.xD()),
            MathUtil.clamp(y, min.yD(), max.yD()),
            MathUtil.clamp(z, min.zD(), max.zD()));
    }
}
