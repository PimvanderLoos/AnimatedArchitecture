package nl.pim16aap2.bigdoors.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

/**
 * Represents an integer x/y/z set.
 * <p>
 * This class is thread-safe, as all modifications return a new instance.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public record Vector3Di(int x, int y, int z) implements Vector3DUtil.IVector3D
{
    /**
     * Creates a new double-based 3d vector from this integer-based 3d vector.
     *
     * @return A new double-based vector.
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
    public Vector3Dd toDouble()
    {
        return Vector3Dd.fromInteger(this);
    }

    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    static Vector3Di fromDouble(Vector3Dd doubleVec)
    {
        return new Vector3Di(round(doubleVec.x()), round(doubleVec.y()), round(doubleVec.z()));
    }

    /**
     * Adds values to the current values.
     *
     * @param x The x value to add to the current x value.
     * @param y The y value to add to the current y value.
     * @param z The z value to add to the current z value.
     * @return A new {@link Vector3Di} with the added values.
     */
    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    public Vector3Di add(int x, int y, int z)
    {
        return new Vector3Di(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Adds another {@link Vector3Di} to the current values.
     *
     * @param other The other {@link Vector3Di} to add to the current one.
     * @return A new {@link Vector3Di} with the added values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di add(Vector3Di other)
    {
        return new Vector3Di(other.x, other.y, other.z);
    }

    /**
     * Adds a value to the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val The value to add to the x, y, and z values.
     * @return A new {@link Vector3Di} with the value added to the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di add(int val)
    {
        return add(val, val, val);
    }


    /**
     * Subtracts values from the current values.
     *
     * @param x The x value to subtract from the current x value.
     * @param y The y value to subtract from the current y value.
     * @param z The z value to subtract from the current z value.
     * @return A new {@link Vector3Di} with the subtracted values.
     */
    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    public Vector3Di subtract(int x, int y, int z)
    {
        return add(-x, -y, -z);
    }

    /**
     * Subtracts another {@link Vector3Di} from the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param other The other {@link Vector3Di} to subtract from the x, y, and z values.
     * @return The new {@link Vector3Di} with the value subtracted from the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di subtract(Vector3Di other)
    {
        return new Vector3Di(other.x, other.y, other.z);
    }

    /**
     * Subtracts a value from the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val The value to subtract from the x, y, and z values.
     * @return A new {@link Vector3Di} with the value subtracted from the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di subtract(int val)
    {
        return add(val, val, val);
    }


    /**
     * Multiplies values with the current values.
     *
     * @param x The x value to multiply with the current x value.
     * @param y The y value to multiply with the current y value.
     * @param z The z value to multiply with the current z value.
     * @return A new {@link Vector3Di} with the multiplied values.
     */
    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    public Vector3Di multiply(int x, int y, int z)
    {
        return new Vector3Di(this.x * x, this.y * y, this.z * z);
    }

    /**
     * Multiplies another {@link Vector3Di} from with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param other The other {@link Vector3Di} to multiply with the x, y, and z values.
     * @return The new {@link Vector3Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di multiply(Vector3Di other)
    {
        return multiply(other.x, other.y, other.z);
    }

    /**
     * Multiplies a value with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val The value to multiply from the x, y, and z values.
     * @return A new {@link Vector3Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di multiply(int val)
    {
        return multiply(val, val, val);
    }


    /**
     * Multiplies values with the current values.
     *
     * @param x The x value to multiply with the current x value.
     * @param y The y value to multiply with the current y value.
     * @param z The z value to multiply with the current z value.
     * @return A new {@link Vector3Di} with the multiplied values.
     */
    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    public Vector3Di multiply(double x, double y, double z)
    {
        return new Vector3Di(round(this.x * x), round(this.y * y), round(this.z * z));
    }

    /**
     * Multiplies another {@link Vector3Dd} from with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param other The other {@link Vector3Dd} to multiply with the x, y, and z values.
     * @return The new {@link Vector3Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di multiply(Vector3Dd other)
    {
        return multiply(other.x(), other.y(), other.z());
    }

    /**
     * Multiplies a value with the x, y, and z values of the current {@link Vector3Di}.
     *
     * @param val The value to multiply from the x, y, and z values.
     * @return A new {@link Vector3Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di multiply(double val)
    {
        return multiply(val, val, val);
    }


    /**
     * Divides the current values with values.
     *
     * @param x The x value to use as divisor for the current x value.
     * @param y The y value to use as divisor for the current y value.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    public Vector3Di divide(int x, int y, int z)
    {
        return new Vector3Di(round(this.x / (double) x), round(this.y / (double) y), round(this.z / (double) z));
    }

    /**
     * Divides the x and y values of the current {@link Vector3Di} with the x, y, and z values of the provided {@link
     * Vector3Di}.
     *
     * @param other The other {@link Vector3Di} to use as divisor for the current x and the y values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di divide(Vector3Di other)
    {
        return divide(other.x, other.y, other.z);
    }

    /**
     * Divides the x, y, and z values of the current {@link Vector3Di} with a provided value.
     *
     * @param val The value to use as divisor for the x, y, and z values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di divide(int val)
    {
        return divide(val, val, val);
    }


    /**
     * Divides the current values with values.
     *
     * @param x The x value to use as divisor for the current x value.
     * @param y The y value to use as divisor for the current y value.
     * @param z The z value to use as divisor for the current z value.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_, _, _ -> new", pure = true)
    public Vector3Di divide(double x, double y, double z)
    {
        return new Vector3Di(round(this.x / x), round(this.y / y), round(this.z / z));
    }

    /**
     * Divides the x and y values of the current {@link Vector3Di} with the x, y, and z values of the provided {@link
     * Vector3Dd}.
     *
     * @param other The other {@link Vector3Dd} to use as divisor for the current x and the y values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di divide(Vector3Dd other)
    {
        return divide(other.x(), other.y(), other.z());
    }

    /**
     * Divides the x, y, and z values of the current {@link Vector3Di} with a provided value.
     *
     * @param val The value to use as divisor for the x, y, and z values.
     * @return A new {@link Vector3Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector3Di divide(double val)
    {
        return divide(val, val, val);
    }

    /**
     * Normalizes this {@link Vector3Di}.
     *
     * @return A new {@link Vector3Di} with normalized
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
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
     * @param pivotPoint The point around which to rotate this point.
     * @param radians    How far to rotate this point (in radians).
     * @return A new {@link Vector3Di} rotated around the pivot point.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector3Di rotateAroundXAxis(Vector3DUtil.IVector3D pivotPoint, double radians)
    {
        return fromDouble(Vector3DUtil.rotateAroundXAxis(this, pivotPoint, radians));
    }

    /**
     * Rotates this point around another point along the y axis.
     *
     * @param pivotPoint The point around which to rotate this point.
     * @param radians    How far to rotate this point (in radians).
     * @return A new {@link Vector3Di} rotated around the pivot point.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector3Di rotateAroundYAxis(Vector3DUtil.IVector3D pivotPoint, double radians)
    {
        return fromDouble(Vector3DUtil.rotateAroundYAxis(this, pivotPoint, radians));
    }

    /**
     * Rotates this point around another point along the z axis.
     *
     * @param pivotPoint The point around which to rotate this point.
     * @param radians    How far to rotate this point (in radians).
     * @return A new {@link Vector3Di} rotated around the pivot point.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector3Di rotateAroundZAxis(Vector3DUtil.IVector3D pivotPoint, double radians)
    {
        return fromDouble(Vector3DUtil.rotateAroundZAxis(this, pivotPoint, radians));
    }

    @CheckReturnValue @Contract(pure = true)
    private static int round(double val)
    {
        return (int) Math.round(val);
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
}
