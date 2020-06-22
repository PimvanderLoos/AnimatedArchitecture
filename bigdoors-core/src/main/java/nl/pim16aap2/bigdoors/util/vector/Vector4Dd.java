package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 4D space.
 *
 * @author Pim
 */
@Data
@AllArgsConstructor
public final class Vector4Dd implements IVector4DdConst, Cloneable
{
    @Getter
    private double x, y, z, w;

    public Vector4Dd(final @NotNull IVector4DdConst other)
    {
        x = other.getX();
        y = other.getY();
        z = other.getZ();
        w = other.getW();
    }

    public Vector4Dd add(final @NotNull IVector4DdConst other)
    {
        add(other.getX(), other.getY(), other.getZ(), other.getW());
        return this;
    }

    public Vector4Dd subtract(final @NotNull IVector4DdConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ(), -other.getW());
        return this;
    }

    public Vector4Dd multiply(final @NotNull IVector4DdConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getZ();
        w *= other.getW();
        return this;
    }

    public Vector4Dd divide(final @NotNull IVector4DdConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getZ();
        w /= other.getW();
        return this;
    }

    public Vector4Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        w *= val;
        return this;
    }

    public Vector4Dd divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        w /= val;
        return this;
    }

    public Vector4Dd addX(double val)
    {
        x += val;
        return this;
    }

    public Vector4Dd addY(double val)
    {
        y += val;
        return this;
    }

    public Vector4Dd addZ(double val)
    {
        z += val;
        return this;
    }

    public Vector4Dd addW(double val)
    {
        w += val;
        return this;
    }

    public Vector4Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    public Vector4Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    public Vector4Dd setZ(double newVal)
    {
        z = newVal;
        return this;
    }

    public Vector4Dd setW(double newVal)
    {
        w = newVal;
        return this;
    }

    public Vector4Dd add(double x, double y, double z, double w)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    @Override
    @NotNull
    public Vector4Dd clone()
    {
        try
        {
            return (Vector4Dd) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }

    @Override
    public String toString()
    {
        return "(" + x + ":" + y + ":" + z + ":" + w + ")";
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 19 * hash + Double.valueOf(x).hashCode();
        hash = 19 * hash + Double.valueOf(y).hashCode();
        hash = 19 * hash + Double.valueOf(z).hashCode();
        hash = 19 * hash + Double.valueOf(w).hashCode();
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
        Vector4Dd other = (Vector4Dd) o;
        return x == other.getX() && y == other.getY() && z == other.getZ() && w == other.getW();
    }

    public Vector4Dd normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);

        x /= length;
        y /= length;
        z /= length;
        w /= length;

        return this;
    }
}
