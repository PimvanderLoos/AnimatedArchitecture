package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an int vector or vertex in 2D space.
 *
 * @author Pim
 */
@Data
@AllArgsConstructor
public final class Vector2Di implements IVector2DiConst, Cloneable
{
    @Getter
    private int x, y;

    public Vector2Di(final @NotNull IVector2DiConst other)
    {
        x = other.getX();
        y = other.getY();
    }

    public Vector2Di add(final @NotNull Vector2Di other)
    {
        add(other.getX(), other.getY());
        return this;
    }

    public Vector2Di subtract(final @NotNull Vector2Di other)
    {
        add(-other.getX(), -other.getY());
        return this;
    }

    public Vector2Di multiply(final @NotNull Vector2Di other)
    {
        x *= other.getX();
        y *= other.getY();
        return this;
    }

    public Vector2Di divide(final @NotNull Vector2Di other)
    {
        x /= other.getX();
        y /= other.getY();
        return this;
    }

    public Vector2Di multiply(final double val)
    {
        x *= val;
        y *= val;
        return this;
    }

    public Vector2Di divide(final double val)
    {
        x /= val;
        y /= val;
        return this;
    }

    public Vector2Di addX(int val)
    {
        x += val;
        return this;
    }

    public Vector2Di addY(int val)
    {
        y += val;
        return this;
    }

    public Vector2Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public Vector2Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public Vector2Di add(int x, int y)
    {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    @NotNull
    public Vector2Di clone()
    {
        try
        {
            return (Vector2Di) super.clone();
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
        hash = 19 * hash + x;
        hash = 19 * hash + y;
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
        Vector2Di other = (Vector2Di) o;
        return x == other.getX() && y == other.getY();
    }

    public Vector2Di normalize()
    {
        double length = Math.sqrt(x * x + y * y);

        x /= length;
        y /= length;

        return this;
    }
}
