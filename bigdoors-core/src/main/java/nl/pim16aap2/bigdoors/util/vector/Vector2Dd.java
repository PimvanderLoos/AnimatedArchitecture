package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a double x/y pair.
 * <p>
 * This class is thread-safe, as all modifications return a new instance.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public record Vector2Dd(double x, double y)
{
    /**
     * Adds two values to the current values.
     *
     * @param x The x value to add to the current x value.
     * @param y The y value to add to the current y value.
     * @return A new {@link Vector2Dd} with the added values.
     */
    public @NotNull Vector2Dd add(double x, double y)
    {
        return new Vector2Dd(this.x + x, this.y + y);
    }

    /**
     * Adds another {@link Vector2Dd} to the current values.
     *
     * @param other The other {@link Vector2Dd} to add to the current one.
     * @return A new {@link Vector2Dd} with the added values.
     */
    public @NotNull Vector2Dd add(final @NotNull Vector2Dd other)
    {
        return new Vector2Dd(other.x, other.y);
    }

    /**
     * Adds another {@link Vector2Di} to the current values.
     *
     * @param other The other {@link Vector2Di} to add to the current one.
     * @return A new {@link Vector2Dd} with the added values.
     */
    public @NotNull Vector2Dd add(final @NotNull Vector2Di other)
    {
        return new Vector2Dd(other.x(), other.y());
    }

    /**
     * Adds a value to both the x and the y axis of the current {@link Vector2Dd}.
     *
     * @param val The value to add to both the x and the y axis.
     * @return A new {@link Vector2Dd} with the value added to the axes.
     */
    public @NotNull Vector2Dd add(double val)
    {
        return add(val, val);
    }


    /**
     * Subtracts two values from the current values.
     *
     * @param x The x value to subtract from the current x value.
     * @param y The y value to subtract from the current y value.
     * @return A new {@link Vector2Dd} with the subtracted values.
     */
    public @NotNull Vector2Dd subtract(double x, double y)
    {
        return add(-x, -y);
    }

    /**
     * Subtracts another {@link Vector2Dd} from both the x and the y axis of the current {@link Vector2Dd}.
     *
     * @param other The other {@link Vector2Dd} to subtract from the x and the y axis.
     * @return The new {@link Vector2Dd} with the value subtracted from the axes.
     */
    public @NotNull Vector2Dd subtract(final @NotNull Vector2Dd other)
    {
        return new Vector2Dd(other.x, other.y);
    }

    /**
     * Subtracts another {@link Vector2Di} from both the x and the y axis of the current {@link Vector2Dd}.
     *
     * @param other The other {@link Vector2Di} to subtract from the x and the y axis.
     * @return The new {@link Vector2Dd} with the value subtracted from the axes.
     */
    public @NotNull Vector2Dd subtract(final @NotNull Vector2Di other)
    {
        return new Vector2Dd(other.x(), other.y());
    }

    /**
     * Subtracts a value from both the x and the y axis of the current {@link Vector2Dd}.
     *
     * @param val The value to subtract from both the x and the y axis.
     * @return A new {@link Vector2Dd} with the value subtracted from the axes.
     */
    public @NotNull Vector2Dd subtract(double val)
    {
        return add(val, val);
    }


    /**
     * Multiplies two values with the current values.
     *
     * @param x The x value to multiply with the current x value.
     * @param y The y value to multiply with the current y value.
     * @return A new {@link Vector2Dd} with the multiplied values.
     */
    public @NotNull Vector2Dd multiply(double x, double y)
    {
        return new Vector2Dd(this.x * x, this.y * y);
    }

    /**
     * Multiplies another {@link Vector2Dd} from with the x and the y values of the current {@link Vector2Dd}.
     *
     * @param other The other {@link Vector2Dd} to multiply with the x and the y values.
     * @return The new {@link Vector2Dd} with the value multiplied with the values.
     */
    public @NotNull Vector2Dd multiply(@NotNull Vector2Dd other)
    {
        return multiply(other.x, other.y);
    }

    /**
     * Multiplies another {@link Vector2Di} from with the x and the y values of the current {@link Vector2Dd}.
     *
     * @param other The other {@link Vector2Di} to multiply with the x and the y values.
     * @return The new {@link Vector2Dd} with the value multiplied with the values.
     */
    public @NotNull Vector2Dd multiply(@NotNull Vector2Di other)
    {
        return multiply(other.x(), other.y());
    }

    /**
     * Multiplies a value with both the x and the y values of the current {@link Vector2Dd}.
     *
     * @param val The value to multiply from both the x and the y values.
     * @return A new {@link Vector2Dd} with the value multiplied with the values.
     */
    public @NotNull Vector2Dd multiply(double val)
    {
        return multiply(val, val);
    }

    /**
     * Divides the current values with two values.
     *
     * @param x The x value to use as divisor for the current x value.
     * @param y The y value to use as divisor for the current y value.
     * @return A new {@link Vector2Dd} with the divided values.
     */
    public @NotNull Vector2Dd divide(double x, double y)
    {
        return new Vector2Dd(this.x / x, this.y / y);
    }

    /**
     * Divides the x and y values of the current {@link Vector2Dd} with the x and the y values of the provided {@link
     * Vector2Dd}.
     *
     * @param other The other {@link Vector2Dd} to use as divisor for the current x and the y values.
     * @return A new {@link Vector2Dd} with the divided values.
     */
    public @NotNull Vector2Dd divide(@NotNull Vector2Dd other)
    {
        return divide(other.x, other.y);
    }

    /**
     * Divides the x and y values of the current {@link Vector2Dd} with the x and the y values of the provided {@link
     * Vector2Di}.
     *
     * @param other The other {@link Vector2Di} to use as divisor for the current x and the y values.
     * @return A new {@link Vector2Dd} with the divided values.
     */
    public @NotNull Vector2Dd divide(@NotNull Vector2Di other)
    {
        return divide(other.x(), other.y());
    }

    /**
     * Divides both the x and the y values of the current {@link Vector2Dd} with a provided value.
     *
     * @param val The value to use as divisor for both the x and the y values.
     * @return A new {@link Vector2Dd} with the divided values.
     */
    public @NotNull Vector2Dd divide(double val)
    {
        return divide(val, val);
    }

    /**
     * Adds a value to the current x value of this {@link Vector2Dd}.
     *
     * @param val The value to add to the current x value.
     * @return A new {@link Vector2Dd} with the provided value added to the x value.
     */
    public @NotNull Vector2Dd addX(double val)
    {
        return add(val, 0);
    }

    /**
     * Adds a value to the current y value of this {@link Vector2Dd}.
     *
     * @param val The value to add to the current y value.
     * @return A new {@link Vector2Dd} with the provided value added to the y value.
     */
    public @NotNull Vector2Dd addY(double val)
    {
        return add(0, val);
    }

    /**
     * Sets the value of the current x value of this {@link Vector2Dd}.
     *
     * @param newVal The new value to use as x value.
     * @return A new {@link Vector2Dd} with the provided value as x value.
     */
    public @NotNull Vector2Dd setX(double newVal)
    {
        return new Vector2Dd(newVal, y);
    }

    /**
     * Sets the value of the current y value of this {@link Vector2Dd}.
     *
     * @param newVal The new value to use as y value.
     * @return A new {@link Vector2Dd} with the provided value as y value.
     */
    public @NotNull Vector2Dd setY(double newVal)
    {
        return new Vector2Dd(x, newVal);
    }


    /**
     * Normalizes this {@link Vector2Dd}.
     *
     * @return A new {@link Vector2Dd} with normalized
     */
    public @NotNull Vector2Dd normalized()
    {
        final double length = Math.sqrt(x * x + y * y);

        final double newX = x / length;
        final double newY = y / length;

        return new Vector2Dd(newX, newY);
    }
}
