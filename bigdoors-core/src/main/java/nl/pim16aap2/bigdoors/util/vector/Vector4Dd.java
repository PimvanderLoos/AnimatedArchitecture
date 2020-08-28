package nl.pim16aap2.bigdoors.util.vector;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 4D space.
 *
 * @author Pim
 */
public final class Vector4Dd extends Vector4DdConst
{
    @Getter
    private double x, y, z, w;

    public Vector4Dd(final double x, final double y, final double z, final double w)
    {
        super(x, y, z, w);
    }

    public Vector4Dd(final @NotNull Vector4DdConst other)
    {
        super(other);
    }

    public Vector4Dd(final @NotNull Vector4DiConst other)
    {
        super(other);
    }

    public @NotNull Vector4Dd add(final @NotNull Vector4DdConst other)
    {
        add(other.getX(), other.getY(), other.getZ(), other.getW());
        return this;
    }

    public @NotNull Vector4Dd subtract(final @NotNull Vector4DdConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ(), -other.getW());
        return this;
    }

    public @NotNull Vector4Dd multiply(final @NotNull Vector4DdConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getZ();
        w *= other.getW();
        return this;
    }

    public @NotNull Vector4Dd divide(final @NotNull Vector4DdConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getZ();
        w /= other.getW();
        return this;
    }

    public @NotNull Vector4Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        w *= val;
        return this;
    }

    public @NotNull Vector4Dd divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        w /= val;
        return this;
    }

    public @NotNull Vector4Dd addX(double val)
    {
        x += val;
        return this;
    }

    public @NotNull Vector4Dd addY(double val)
    {
        y += val;
        return this;
    }

    public @NotNull Vector4Dd addZ(double val)
    {
        z += val;
        return this;
    }

    public @NotNull Vector4Dd addW(double val)
    {
        w += val;
        return this;
    }

    public @NotNull Vector4Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    public @NotNull Vector4Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    public @NotNull Vector4Dd setZ(double newVal)
    {
        z = newVal;
        return this;
    }

    public @NotNull Vector4Dd setW(double newVal)
    {
        w = newVal;
        return this;
    }

    public @NotNull Vector4Dd add(double x, double y, double z, double w)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    @Override
    public @NotNull Vector4Dd clone()
    {
        return new Vector4Dd(this);
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
        Vector4Dd other = (Vector4Dd) o;
        return x == other.getX() && y == other.getY() && z == other.getZ() && w == other.getW();
    }

    public @NotNull Vector4Dd normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);

        x /= length;
        y /= length;
        z /= length;
        w /= length;

        return this;
    }
}
