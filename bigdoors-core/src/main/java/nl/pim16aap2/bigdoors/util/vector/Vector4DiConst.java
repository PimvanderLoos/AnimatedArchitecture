package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public class Vector4DiConst
{
    @Getter
    protected int x, y, z, w;

    public Vector4DiConst(final @NonNull Vector4DiConst other)
    {
        this(other.getX(), other.getY(), other.getZ(), other.getW());
    }

    @Override
    public @NonNull Vector4Di clone()
    {
        return new Vector4Di(this);
    }

    @Override
    public @NonNull String toString()
    {
        return "(" + x + ":" + y + ":" + z + ":" + w + ")";
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 19 * hash + x;
        hash = 19 * hash + y;
        hash = 19 * hash + z;
        hash = 19 * hash + w;
        return hash;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof Vector4DiConst))
            return false;

        Vector4DiConst other = (Vector4DiConst) o;
        return x == other.getX() && y == other.getY() && z == other.getZ() && w == other.getW();
    }
}
