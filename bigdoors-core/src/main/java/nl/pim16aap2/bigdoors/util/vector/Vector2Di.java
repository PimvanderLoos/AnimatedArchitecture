package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an int vector or vertex in 2D space.
 *
 * @author Pim
 */
public final class Vector2Di extends Vector2DiConst
{
    public Vector2Di(final int x, final int y)
    {
        super(x, y);
    }

    public Vector2Di(final @NotNull Vector2DiConst other)
    {
        super(other);
    }

    public @NotNull Vector2Di add(final @NotNull Vector2Di other)
    {
        add(other.getX(), other.getY());
        return this;
    }

    public @NotNull Vector2Di subtract(final @NotNull Vector2Di other)
    {
        add(-other.getX(), -other.getY());
        return this;
    }

    public @NotNull Vector2Di multiply(final @NotNull Vector2Di other)
    {
        x *= other.getX();
        y *= other.getY();
        return this;
    }

    public @NotNull Vector2Di divide(final @NotNull Vector2Di other)
    {
        x /= other.getX();
        y /= other.getY();
        return this;
    }

    public @NotNull Vector2Di multiply(final int val)
    {
        x *= val;
        y *= val;
        return this;
    }

    public @NotNull Vector2Di divide(final int val)
    {
        x /= val;
        y /= val;
        return this;
    }

    public @NotNull Vector2Di addX(int val)
    {
        x += val;
        return this;
    }

    public @NotNull Vector2Di addY(int val)
    {
        y += val;
        return this;
    }

    public @NotNull Vector2Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public @NotNull Vector2Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public @NotNull Vector2Di add(int x, int y)
    {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    public @NotNull Vector2Di clone()
    {
        return new Vector2Di(this);
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

    public @NotNull Vector2Di normalize()
    {
        double length = Math.sqrt(x * x + y * y);

        x /= length;
        y /= length;

        return this;
    }
}
