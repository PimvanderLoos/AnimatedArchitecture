package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents an integer vector or vertex in 2D space.
 *
 * @author Pim
 */
@AllArgsConstructor
public class Vector2DiConst
{
    @Getter
    protected int x, y;

    public Vector2DiConst(final @NonNull Vector2DiConst other)
    {
        this(other.getX(), other.getY());
    }

    @Override
    public @NonNull Vector2Di clone()
    {
        return new Vector2Di(this);
    }

    @Override
    public @NonNull String toString()
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

        if (!(o instanceof Vector2DiConst))
            return false;

        Vector2DiConst other = (Vector2DiConst) o;
        return x == other.getX() && y == other.getY();
    }
}
