package nl.pim16aap2.bigdoors.util.vector;


import lombok.NonNull;

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

    public Vector2Di(final @NonNull Vector2DiConst other)
    {
        super(other);
    }

    public @NonNull Vector2Di add(final @NonNull Vector2Di other)
    {
        add(other.getX(), other.getY());
        return this;
    }

    public @NonNull Vector2Di subtract(final @NonNull Vector2Di other)
    {
        add(-other.getX(), -other.getY());
        return this;
    }

    public @NonNull Vector2Di multiply(final @NonNull Vector2Di other)
    {
        x *= other.getX();
        y *= other.getY();
        return this;
    }

    public @NonNull Vector2Di divide(final @NonNull Vector2Di other)
    {
        x /= other.getX();
        y /= other.getY();
        return this;
    }

    public @NonNull Vector2Di multiply(final int val)
    {
        x *= val;
        y *= val;
        return this;
    }

    public @NonNull Vector2Di divide(final int val)
    {
        x /= val;
        y /= val;
        return this;
    }

    public @NonNull Vector2Di addX(int val)
    {
        x += val;
        return this;
    }

    public @NonNull Vector2Di addY(int val)
    {
        y += val;
        return this;
    }

    public @NonNull Vector2Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public @NonNull Vector2Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public @NonNull Vector2Di add(int x, int y)
    {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    public @NonNull Vector2Di clone()
    {
        return new Vector2Di(this);
    }

    public @NonNull Vector2Di normalize()
    {
        double length = Math.sqrt(x * x + y * y);

        x /= length;
        y /= length;

        return this;
    }
}
