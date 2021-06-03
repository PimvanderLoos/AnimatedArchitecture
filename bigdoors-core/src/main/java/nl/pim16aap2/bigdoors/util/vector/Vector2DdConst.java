package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 2D space.
 *
 * @author Pim
 */
@AllArgsConstructor
public class Vector2DdConst
{
    @Getter
    protected double x, y;

    public Vector2DdConst(final @NotNull Vector2DdConst other)
    {
        this(other.getX(), other.getY());
    }

    public Vector2DdConst(final @NotNull Vector2DiConst other)
    {
        this(other.getX(), other.getY());
    }

    @Override
    public @NotNull Vector2Dd clone()
    {
        return new Vector2Dd(this);
    }

    /**
     * Converts this object to a String to a certain number of decimal places.
     *
     * @param decimals The number of digits after the dot to display.
     * @return The String representing this object.
     */
    public @NotNull String toString(final int decimals)
    {
        final @NotNull String placeholder = "%." + decimals + "f";
        return String.format(placeholder + ", " + placeholder, x, y);
    }

    @Override
    public @NotNull String toString()
    {
        return "(" + x + ":" + y + ")";
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 19 * hash + Double.valueOf(x).hashCode();
        hash = 19 * hash + Double.valueOf(y).hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof Vector2DdConst))
            return false;

        Vector2DdConst other = (Vector2DdConst) o;
        return x == other.getX() && y == other.getY();
    }
}
