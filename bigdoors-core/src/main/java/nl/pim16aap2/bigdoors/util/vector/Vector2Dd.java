package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 2D space.
 *
 * @author Pim
 */
public class Vector2Dd extends Vector2DdConst
{
    public Vector2Dd(final double x, final double y)
    {
        super(x, y);
    }

    public Vector2Dd(final @NotNull Vector2DdConst other)
    {
        super(other);
    }

    public Vector2Dd(final @NotNull Vector2DiConst other)
    {
        super(other);
    }

    public @NotNull Vector2Dd add(final @NotNull Vector2Dd other)
    {
        add(other.getX(), other.getY());
        return this;
    }

    public @NotNull Vector2Dd subtract(final @NotNull Vector2Dd other)
    {
        add(-other.getX(), -other.getY());
        return this;
    }

    public @NotNull Vector2Dd multiply(final @NotNull Vector2Dd other)
    {
        x *= other.getX();
        y *= other.getY();
        return this;
    }

    public @NotNull Vector2Dd divide(final @NotNull Vector2Dd other)
    {
        x /= other.getX();
        y /= other.getY();
        return this;
    }

    public @NotNull Vector2Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        return this;
    }

    public @NotNull Vector2Dd divide(final double val)
    {
        x /= val;
        y /= val;
        return this;
    }

    public @NotNull Vector2Dd addX(double val)
    {
        x += val;
        return this;
    }

    public @NotNull Vector2Dd addY(double val)
    {
        y += val;
        return this;
    }

    public @NotNull Vector2Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    public @NotNull Vector2Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    public @NotNull Vector2Dd add(double x, double y)
    {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    public @NotNull Vector2Dd clone()
    {
        return new Vector2Dd(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        Vector2Dd other = (Vector2Dd) o;
        return x == other.getX() && y == other.getY();
    }

    public @NotNull Vector2Dd normalize()
    {
        double length = Math.sqrt(x * x + y * y);

        x /= length;
        y /= length;

        return this;
    }
}
