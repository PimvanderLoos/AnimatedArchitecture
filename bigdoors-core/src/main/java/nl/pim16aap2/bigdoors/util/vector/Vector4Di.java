package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an int vector or vertex in 4D space.
 *
 * @author Pim
 */
public final class Vector4Di extends Vector4DiConst
{
    public Vector4Di(final int x, final int y, final int z, final int w)
    {
        super(x, y, z, w);
    }

    public Vector4Di(final @NotNull Vector4DiConst other)
    {
        super(other);
    }

    public @NotNull Vector4Di add(final @NotNull Vector4DiConst other)
    {
        add(other.getX(), other.getY(), other.getZ(), other.getW());
        return this;
    }

    public @NotNull Vector4Di subtract(final @NotNull Vector4DiConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ(), -other.getW());
        return this;
    }

    public @NotNull Vector4Di multiply(final @NotNull Vector4DiConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getY();
        w *= other.getY();
        return this;
    }

    public @NotNull Vector4Di divide(final @NotNull Vector4DiConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getY();
        w /= other.getY();
        return this;
    }

    public @NotNull Vector4Di multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        w *= val;
        return this;
    }

    public @NotNull Vector4Di divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        w /= val;
        return this;
    }

    public @NotNull Vector4Di addX(int val)
    {
        x += val;
        return this;
    }

    public @NotNull Vector4Di addY(int val)
    {
        y += val;
        return this;
    }

    public @NotNull Vector4Di addZ(int val)
    {
        z += val;
        return this;
    }

    public @NotNull Vector4Di addW(int val)
    {
        w += val;
        return this;
    }

    public @NotNull Vector4Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public @NotNull Vector4Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public @NotNull Vector4Di setZ(int newVal)
    {
        z = newVal;
        return this;
    }

    public @NotNull Vector4Di setW(int newVal)
    {
        w = newVal;
        return this;
    }

    public @NotNull Vector4Di add(int x, int y, int z, int w)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    @Override
    public @NotNull Vector4Di clone()
    {
        return new Vector4Di(this);
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
        Vector4Di other = (Vector4Di) o;
        return x == other.getX() && y == other.getY() && z == other.getZ() && w == other.getW();
    }

    public @NotNull Vector4Di normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);

        x /= length;
        y /= length;
        z /= length;
        w /= length;

        return this;
    }
}
