package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 2D space.
 *
 * @author Pim
 */
@Data
@AllArgsConstructor
public final class Vector2Dd implements IVector2DdConst, Cloneable
{
    @Getter
    private double x, y;

    public Vector2Dd(final @NotNull IVector2DdConst other)
    {
        x = other.getX();
        y = other.getY();
    }

    @NotNull
    public Vector2Dd add(final @NotNull Vector2Dd other)
    {
        add(other.getX(), other.getY());
        return this;
    }

    @NotNull
    public Vector2Dd subtract(final @NotNull Vector2Dd other)
    {
        add(-other.getX(), -other.getY());
        return this;
    }

    @NotNull
    public Vector2Dd multiply(final @NotNull Vector2Dd other)
    {
        x *= other.getX();
        y *= other.getY();
        return this;
    }

    @NotNull
    public Vector2Dd divide(final @NotNull Vector2Dd other)
    {
        x /= other.getX();
        y /= other.getY();
        return this;
    }

    @NotNull
    public Vector2Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        return this;
    }

    @NotNull
    public Vector2Dd divide(final double val)
    {
        x /= val;
        y /= val;
        return this;
    }

    @NotNull
    public Vector2Dd addX(double val)
    {
        x += val;
        return this;
    }

    @NotNull
    public Vector2Dd addY(double val)
    {
        y += val;
        return this;
    }

    @NotNull
    public Vector2Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    @NotNull
    public Vector2Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    @NotNull
    public Vector2Dd add(double x, double y)
    {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    public @NotNull Vector2Dd clone()
    {
        try
        {
            return (Vector2Dd) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // TODO: Only log to file! It's already dumped in the console because it's thrown.
            Error er = new Error(e);
            PLogger.get().logError(er);
            throw er;
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
