package nl.pim16aap2.bigdoors.util.vector;


import lombok.NonNull;

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

    public Vector2Dd(final @NonNull Vector2DdConst other)
    {
        super(other);
    }

    public Vector2Dd(final @NonNull Vector2DiConst other)
    {
        super(other);
    }

    public @NonNull Vector2Dd add(final @NonNull Vector2Dd other)
    {
        add(other.getX(), other.getY());
        return this;
    }

    public @NonNull Vector2Dd subtract(final @NonNull Vector2Dd other)
    {
        add(-other.getX(), -other.getY());
        return this;
    }

    public @NonNull Vector2Dd multiply(final @NonNull Vector2Dd other)
    {
        x *= other.getX();
        y *= other.getY();
        return this;
    }

    public @NonNull Vector2Dd divide(final @NonNull Vector2Dd other)
    {
        x /= other.getX();
        y /= other.getY();
        return this;
    }

    public @NonNull Vector2Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        return this;
    }

    public @NonNull Vector2Dd divide(final double val)
    {
        x /= val;
        y /= val;
        return this;
    }

    public @NonNull Vector2Dd addX(double val)
    {
        x += val;
        return this;
    }

    public @NonNull Vector2Dd addY(double val)
    {
        y += val;
        return this;
    }

    public @NonNull Vector2Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    public @NonNull Vector2Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    public @NonNull Vector2Dd add(double x, double y)
    {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    public @NonNull Vector2Dd clone()
    {
        return new Vector2Dd(this);
    }

    public @NonNull Vector2Dd normalize()
    {
        double length = Math.sqrt(x * x + y * y);

        x /= length;
        y /= length;

        return this;
    }
}
