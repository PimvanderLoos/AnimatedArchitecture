package nl.pim16aap2.animatedarchitecture.core.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.jetbrains.annotations.Contract;

/**
 * Represents an integer x/y/z set.
 * <p>
 * This class is thread-safe, as all modifications return a new instance.
 */
public record Vector3Di(int x, int y, int z) implements IVector3D
{
    public Vector3Di(IVector3D other)
    {
        this(round(other.xD()), round(other.yD()), round(other.zD()));
    }

    /**
     * Creates a {@link Vector3Di} from a {@link IVector3D}.
     * <p>
     * If the provided {@link IVector3D} is already a {@link Vector3Di}, the same instance is returned, otherwise a new
     * instance is created by rounding the x/y/z values to the nearest integer.
     *
     * @param other
     *     A {@link Vector3Di} to retrieve as {@link Vector3Di}
     * @return A {@link Vector3Di} representing the provided {@link Vector3Di}. If the provided {@link Vector3Di} was
     * already of this type, the same instance is returned.
     */
    public static Vector3Di of(IVector3D other)
    {
        return other instanceof Vector3Di vec3d ? vec3d : new Vector3Di(other);
    }

    @Override
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di toInteger()
    {
        return this;
    }

    @Override
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Dd toDouble()
    {
        return Vector3Dd.fromInteger(this);
    }

    @CheckReturnValue
    @Contract(pure = true)
    static Vector3Di fromDouble(Vector3Dd doubleVec)
    {
        return new Vector3Di(round(doubleVec.x()), round(doubleVec.y()), round(doubleVec.z()));
    }

    /**
     * Adds integer values to the current values.
     *
     * @param x
     *     The x value to add to the current x value.
     * @param y
     *     The y value to add to the current y value.
     * @param z
     *     The z value to add to the current z value.
     * @return A new {@link Vector3Di} with the added values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di add(int x, int y, int z)
    {
        return new Vector3Di(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Adds floating point values to the current x/y/z.
     *
     * @param x
     *     The x value to add to the current x value.
     * @param y
     *     The y value to add to the current y value.
     * @param z
     *     The z value to add to the current z value.
     * @return A new {@link Vector3Dd} with the added values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Dd add(double x, double y, double z)
    {
        return new Vector3Dd(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Adds another {@link Vector3Di} to the current values.
     *
     * @param other
     *     The other {@link Vector3Di} to add to the current one.
     * @return A new {@link Vector3Di} with the added values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di add(Vector3Di other)
    {
        return add(other.x, other.y, other.z);
    }

    /**
     * Adds another {@link Vector3Dd} to the current values.
     *
     * @param other
     *     The other {@link Vector3Dd} to add to the current one.
     * @return A new {@link Vector3Dd} with the added values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Dd add(Vector3Dd other)
    {
        return add(other.x(), other.y(), other.z());
    }

    /**
     * Adds an integer value to the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val
     *     The integer value to add to the x, y, and z values.
     * @return A new {@link Vector3Di} with the value added to the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di add(int val)
    {
        return add(val, val, val);
    }

    /**
     * Adds a floating point value to the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val
     *     The floating point value to add to the x, y, and z values.
     * @return A new {@link Vector3Dd} with the value added to the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
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
     * @return A new {@link Vector3Di} with the subtracted values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di subtract(int x, int y, int z)
    {
        return add(-x, -y, -z);
    }

    /**
     * Subtracts floating point values from the current values.
     *
     * @param x
     *     The x value to subtract from the current x value.
     * @param y
     *     The y value to subtract from the current y value.
     * @param z
     *     The z value to subtract from the current z value.
     * @return A new {@link Vector3Dd} with the subtracted values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Dd subtract(double x, double y, double z)
    {
        return add(-x, -y, -z);
    }

    /**
     * Subtracts another {@link Vector3Dd} from the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param other
     *     The other {@link Vector3Dd} to subtract from the x, y, and z values.
     * @return The new {@link Vector3Dd} with the value subtracted from the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Dd subtract(Vector3Dd other)
    {
        return subtract(other.x(), other.y(), other.z());
    }

    /**
     * Subtracts another {@link Vector3Di} from the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param other
     *     The other {@link Vector3Di} to subtract from the x, y, and z values.
     * @return The new {@link Vector3Di} with the value subtracted from the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di subtract(Vector3Di other)
    {
        return subtract(other.x, other.y, other.z);
    }

    /**
     * Subtracts a value from the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val
     *     The value to subtract from the x, y, and z values.
     * @return A new {@link Vector3Di} with the value subtracted from the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di subtract(int val)
    {
        return subtract(val, val, val);
    }

    /**
     * Subtracts a floating point value from the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val
     *     The floating point  value to subtract from the x, y, and z values.
     * @return A new {@link Vector3Dd} with the value subtracted from the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
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
     * @return A new {@link Vector3Di} with the multiplied values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di multiply(int x, int y, int z)
    {
        return new Vector3Di(this.x * x, this.y * y, this.z * z);
    }

    /**
     * Multiplies another {@link Vector3Di} from with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param other
     *     The other {@link Vector3Di} to multiply with the x, y, and z values.
     * @return The new {@link Vector3Di} with the value multiplied with the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di multiply(Vector3Di other)
    {
        return multiply(other.x, other.y, other.z);
    }

    /**
     * Multiplies a value with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val
     *     The value to multiply from the x, y, and z values.
     * @return A new {@link Vector3Di} with the value multiplied with the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di multiply(int val)
    {
        return multiply(val, val, val);
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
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Dd multiply(double x, double y, double z)
    {
        return new Vector3Dd(this.x * x, this.y * y, this.z * z);
    }

    /**
     * Multiplies another {@link Vector3Dd} from with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param other
     *     The other {@link Vector3Dd} to multiply with the x, y, and z values.
     * @return The new {@link Vector3Dd} with the value multiplied with the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Dd multiply(Vector3Dd other)
    {
        return multiply(other.x(), other.y(), other.z());
    }

    /**
     * Multiplies a value with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val
     *     The value to multiply from the x, y, and z values.
     * @return A new {@link Vector3Dd} with the value multiplied with the values.
     */
    @CheckReturnValue
    @Contract(pure = true)
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
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di divide(int x, int y, int z)
    {
        return new Vector3Di(round(this.x / (double) x), round(this.y / (double) y), round(this.z / (double) z));
    }

    /**
     * Divides the x and y values of the current {@link Vector3Di} with the x, y, and z values of the provided
     * {@link Vector3Di}.
     *
     * @param other
     *     The other {@link Vector3Di} to use as divisor for the current x and the y values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di divide(Vector3Di other)
    {
        return divide(other.x, other.y, other.z);
    }

    /**
     * Divides the x, y, and z values of the current {@link Vector3Di} with a provided value.
     *
     * @param val
     *     The value to use as divisor for the x, y, and z values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di divide(int val)
    {
        return divide(val, val, val);
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
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di divide(double x, double y, double z)
    {
        return new Vector3Di(round(this.x / x), round(this.y / y), round(this.z / z));
    }

    /**
     * Divides the x and y values of the current {@link Vector3Di} with the x, y, and z values of the provided
     * {@link Vector3Dd}.
     *
     * @param other
     *     The other {@link Vector3Dd} to use as divisor for the current x and the y values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di divide(Vector3Dd other)
    {
        return divide(other.x(), other.y(), other.z());
    }

    /**
     * Divides the x, y, and z values of the current {@link Vector3Di} with a provided value.
     *
     * @param val
     *     The value to use as divisor for the x, y, and z values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di divide(double val)
    {
        return divide(val, val, val);
    }

    /**
     * Normalizes this {@link Vector3Di}.
     *
     * @return A new, normalized, {@link Vector3Di}.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di normalized()
    {
        final double length = Math.sqrt((double) x * x + y * y + z * z);

        final int newX = round(x / length);
        final int newY = round(y / length);
        final int newZ = round(z / length);

        return new Vector3Di(newX, newY, newZ);
    }

    /**
     * Rotates this point around another point along the x axis.
     *
     * @param pivotPoint
     *     The point around which to rotate this point.
     * @param radians
     *     How far to rotate this point (in radians).
     * @return A new {@link Vector3Di} rotated around the pivot point.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di rotateAroundXAxis(IVector3D pivotPoint, double radians)
    {
        return fromDouble(Vector3DUtil.rotateAroundXAxis(this, pivotPoint, radians));
    }

    /**
     * Rotates this point around another point along the y axis.
     *
     * @param pivotPoint
     *     The point around which to rotate this point.
     * @param radians
     *     How far to rotate this point (in radians).
     * @return A new {@link Vector3Di} rotated around the pivot point.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di rotateAroundYAxis(IVector3D pivotPoint, double radians)
    {
        return fromDouble(Vector3DUtil.rotateAroundYAxis(this, pivotPoint, radians));
    }

    /**
     * Rotates this point around another point along the z axis.
     *
     * @param pivotPoint
     *     The point around which to rotate this point.
     * @param radians
     *     How far to rotate this point (in radians).
     * @return A new {@link Vector3Di} rotated around the pivot point.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di rotateAroundZAxis(IVector3D pivotPoint, double radians)
    {
        return fromDouble(Vector3DUtil.rotateAroundZAxis(this, pivotPoint, radians));
    }

    @CheckReturnValue
    @Contract(pure = true)
    private static int round(double val)
    {
        return MathUtil.round(val);
    }

    /**
     * Signed bitwise left shift the values in this vector.
     *
     * @param bits
     *     The number of bits to shift the values.
     * @return A new {@link Vector3Di}.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di leftShift(int bits)
    {
        return new Vector3Di(x << bits, y << bits, z << bits);
    }

    /**
     * Signed bitwise right shift the values in this vector.
     *
     * @param bits
     *     The number of bits to shift the values.
     * @return A new {@link Vector3Di}.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di rightShift(int bits)
    {
        return new Vector3Di(x >> bits, y >> bits, z >> bits);
    }

    /**
     * Gets the absolute values of this vector.
     *
     * @return A new {@link Vector3Di}.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public Vector3Di absolute()
    {
        return new Vector3Di(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    /**
     * Returns the highest value in this vector.
     *
     * @return The highest value in this vector.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public int max()
    {
        return Math.max(x, Math.max(y, z));
    }

    /**
     * Returns the lowest value in this vector.
     *
     * @return The lowest value in this vector.
     */
    @CheckReturnValue
    @Contract(pure = true)
    public int min()
    {
        return Math.min(x, Math.min(y, z));
    }

    /**
     * Returns the sum of the x, y, and z values.
     *
     * @return The sum of the x, y, and z values.
     */
    public int sum()
    {
        return x + y + z;
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
    public Vector3Di floor()
    {
        return this;
    }

    @Override
    public Vector3Di round()
    {
        return this;
    }

    @Override
    public Vector3Di ceil()
    {
        return this;
    }
}
