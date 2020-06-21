package nl.pim16aap2.bigdoors.util.vector;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 2D space.
 *
 * @author Pim
 */
public final class Vector2Dd implements Cloneable
{
    @Getter
    private double x, y;

    public Vector2Dd(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public Vector2Dd(final @NotNull Vector2Dd other)
    {
        x = other.x;
        y = other.y;
    }

    public Vector2Dd add(final @NotNull Vector2Dd other)
    {
        add(other.x, other.y);
        return this;
    }

    public Vector2Dd subtract(final @NotNull Vector2Dd other)
    {
        add(-other.x, -other.y);
        return this;
    }

    public Vector2Dd multiply(final @NotNull Vector2Dd other)
    {
        x *= other.x;
        y *= other.y;
        return this;
    }

    public Vector2Dd divide(final @NotNull Vector2Dd other)
    {
        x /= other.x;
        y /= other.y;
        return this;
    }

    public Vector2Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        return this;
    }

    public Vector2Dd divide(final double val)
    {
        x /= val;
        y /= val;
        return this;
    }

    public Vector2Dd addX(double val)
    {
        x += val;
        return this;
    }

    public Vector2Dd addY(double val)
    {
        y += val;
        return this;
    }

    public Vector2Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    public Vector2Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    public Vector2Dd add(double x, double y)
    {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    @NotNull
    public Vector2Dd clone()
    {
        try
        {
            return (Vector2Dd) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }

    @Override
    public String toString()
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
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        Vector2Dd other = (Vector2Dd) o;
        return x == other.x && y == other.y;
    }

    public Vector2Dd normalize()
    {
        double length = Math.sqrt(x * x + y * y);

        x /= length;
        y /= length;

        return this;
    }
}
