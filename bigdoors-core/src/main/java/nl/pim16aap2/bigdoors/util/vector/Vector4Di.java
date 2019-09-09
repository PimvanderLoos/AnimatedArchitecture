package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an int vector or vertex in 4D space.
 *
 * @author Pim
 */
public final class Vector4Di implements Cloneable
{
    private int x, y, z, w;

    public Vector4Di(int x, int y, int z, int w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4Di(final @NotNull Vector4Di other)
    {
        x = other.x;
        y = other.y;
        z = other.z;
        w = other.w;
    }

    public Vector4Di add(final @NotNull Vector4Di other)
    {
        add(other.x, other.y, other.z, other.w);
        return this;
    }

    public Vector4Di subtract(final @NotNull Vector4Di other)
    {
        add(-other.x, -other.y, -other.z, -other.w);
        return this;
    }

    public Vector4Di multiply(final @NotNull Vector4Di other)
    {
        x *= other.x;
        y *= other.y;
        z *= other.y;
        w *= other.y;
        return this;
    }

    public Vector4Di divide(final @NotNull Vector4Di other)
    {
        x /= other.x;
        y /= other.y;
        z /= other.y;
        w /= other.y;
        return this;
    }

    public Vector4Di multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        w *= val;
        return this;
    }

    public Vector4Di divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        w /= val;
        return this;
    }

    public Vector4Di addX(int val)
    {
        x += val;
        return this;
    }

    public Vector4Di addY(int val)
    {
        y += val;
        return this;
    }

    public Vector4Di addZ(int val)
    {
        z += val;
        return this;
    }

    public Vector4Di addW(int val)
    {
        w += val;
        return this;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getZ()
    {
        return z;
    }

    public int getW()
    {
        return w;
    }

    public Vector4Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public Vector4Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public Vector4Di setZ(int newVal)
    {
        z = newVal;
        return this;
    }

    public Vector4Di setW(int newVal)
    {
        w = newVal;
        return this;
    }

    public Vector4Di add(int x, int y, int z, int w)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    @Override
    @NotNull
    public Vector4Di clone()
    {
        try
        {
            return (Vector4Di) super.clone();
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
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        Vector4Di other = (Vector4Di) o;
        return x == other.x && y == other.y && z == other.z && w == other.w;
    }

    public Vector4Di normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);

        x /= length;
        y /= length;
        z /= length;
        w /= length;

        return this;
    }
}
