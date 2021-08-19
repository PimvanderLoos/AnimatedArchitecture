package nl.pim16aap2.bigdoors.util.vector;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

/**
 * Represents an integer x/y pair.
 * <p>
 * This class is thread-safe, as all modifications return a new instance.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public record Vector2Di(int x, int y)
{
    /**
     * Creates a new double-based 2d vector from this integer-based 2d vector.
     *
     * @return A new integer-based vector.
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
    public Vector2Dd toDouble()
    {
        return Vector2Dd.fromInteger(this);
    }

    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    static Vector2Di fromDouble(Vector2Dd doubleVec)
    {
        return new Vector2Di(round(doubleVec.x()), round(doubleVec.y()));
    }

    /**
     * Adds values to the current values.
     *
     * @param x The x value to add to the current x value.
     * @param y The y value to add to the current y value.
     * @return A new {@link Vector2Di} with the added values.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector2Di add(int x, int y)
    {
        return new Vector2Di(this.x + x, this.y + y);
    }

    /**
     * Adds another {@link Vector2Di} to the current values.
     *
     * @param other The other {@link Vector2Di} to add to the current one.
     * @return A new {@link Vector2Di} with the added values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di add(final Vector2Di other)
    {
        return new Vector2Di(other.x, other.y);
    }

    /**
     * Adds a value to both the x and the y values of the current {@link Vector2Di}.
     *
     * @param val The value to add to both the x and the y values.
     * @return A new {@link Vector2Di} with the value added to the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di add(int val)
    {
        return add(val, val);
    }


    /**
     * Subtracts values from the current values.
     *
     * @param x The x value to subtract from the current x value.
     * @param y The y value to subtract from the current y value.
     * @return A new {@link Vector2Di} with the subtracted values.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector2Di subtract(int x, int y)
    {
        return add(-x, -y);
    }

    /**
     * Subtracts another {@link Vector2Di} from both the x and the y values of the current {@link Vector2Di}.
     *
     * @param other The other {@link Vector2Di} to subtract from the x and the y values.
     * @return The new {@link Vector2Di} with the value subtracted from the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di subtract(final Vector2Di other)
    {
        return new Vector2Di(other.x, other.y);
    }

    /**
     * Subtracts a value from both the x and the y values of the current {@link Vector2Di}.
     *
     * @param val The value to subtract from both the x and the y values.
     * @return A new {@link Vector2Di} with the value subtracted from the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di subtract(int val)
    {
        return add(val, val);
    }


    /**
     * Multiplies values with the current values.
     *
     * @param x The x value to multiply with the current x value.
     * @param y The y value to multiply with the current y value.
     * @return A new {@link Vector2Di} with the multiplied values.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector2Di multiply(int x, int y)
    {
        return new Vector2Di(this.x * x, this.y * y);
    }

    /**
     * Multiplies another {@link Vector2Di} from with the x and the y values of the current {@link Vector2Di}.
     *
     * @param other The other {@link Vector2Di} to multiply with the x and the y values.
     * @return The new {@link Vector2Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di multiply(Vector2Di other)
    {
        return multiply(other.x, other.y);
    }

    /**
     * Multiplies a value with both the x and the y values of the current {@link Vector2Di}.
     *
     * @param val The value to multiply from both the x and the y values.
     * @return A new {@link Vector2Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di multiply(int val)
    {
        return multiply(val, val);
    }


    /**
     * Multiplies values with the current values.
     *
     * @param x The x value to multiply with the current x value.
     * @param y The y value to multiply with the current y value.
     * @return A new {@link Vector2Di} with the multiplied values.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector2Di multiply(double x, double y)
    {
        return new Vector2Di(round(this.x * x), round(this.y * y));
    }

    /**
     * Multiplies another {@link Vector2Dd} from with the x and the y values of the current {@link Vector2Di}.
     *
     * @param other The other {@link Vector2Dd} to multiply with the x and the y values.
     * @return The new {@link Vector2Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di multiply(Vector2Dd other)
    {
        return multiply(other.x(), other.y());
    }

    /**
     * Multiplies a value with both the x and the y values of the current {@link Vector2Di}.
     *
     * @param val The value to multiply from both the x and the y values.
     * @return A new {@link Vector2Di} with the value multiplied with the values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di multiply(double val)
    {
        return multiply(val, val);
    }


    /**
     * Divides the current values with values.
     *
     * @param x The x value to use as divisor for the current x value.
     * @param y The y value to use as divisor for the current y value.
     * @return A new {@link Vector2Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector2Di divide(int x, int y)
    {
        return new Vector2Di(round(this.x / (double) x), round(this.y / (double) y));
    }

    /**
     * Divides the x and y values of the current {@link Vector2Di} with the x and the y values of the provided {@link
     * Vector2Di}.
     *
     * @param other The other {@link Vector2Di} to use as divisor for the current x and the y values.
     * @return A new {@link Vector2Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di divide(Vector2Di other)
    {
        return divide(other.x, other.y);
    }

    /**
     * Divides both the x and the y values of the current {@link Vector2Di} with a provided value.
     *
     * @param val The value to use as divisor for both the x and the y values.
     * @return A new {@link Vector2Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di divide(int val)
    {
        return divide(val, val);
    }


    /**
     * Divides the current values with values.
     *
     * @param x The x value to use as divisor for the current x value.
     * @param y The y value to use as divisor for the current y value.
     * @return A new {@link Vector2Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_, _ -> new", pure = true)
    public Vector2Di divide(double x, double y)
    {
        return new Vector2Di(round(this.x / x), round(this.y / y));
    }

    /**
     * Divides the x and y values of the current {@link Vector2Di} with the x and the y values of the provided {@link
     * Vector2Dd}.
     *
     * @param other The other {@link Vector2Dd} to use as divisor for the current x and the y values.
     * @return A new {@link Vector2Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di divide(Vector2Dd other)
    {
        return divide(other.x(), other.y());
    }

    /**
     * Divides both the x and the y values of the current {@link Vector2Di} with a provided value.
     *
     * @param val The value to use as divisor for both the x and the y values.
     * @return A new {@link Vector2Di} with the divided values.
     */
    @CheckReturnValue @Contract(value = "_ -> new", pure = true)
    public Vector2Di divide(double val)
    {
        return divide(val, val);
    }


    /**
     * Normalizes this {@link Vector2Di}.
     *
     * @return A new {@link Vector2Di} with normalized
     */
    @CheckReturnValue @Contract(value = " -> new", pure = true)
    public Vector2Di normalized()
    {
        final double length = Math.sqrt((double) x * x + y * y);

        final int newX = round(x / length);
        final int newY = round(y / length);

        return new Vector2Di(newX, newY);
    }

    private static int round(double val)
    {
        return (int) Math.round(val);
    }
}
