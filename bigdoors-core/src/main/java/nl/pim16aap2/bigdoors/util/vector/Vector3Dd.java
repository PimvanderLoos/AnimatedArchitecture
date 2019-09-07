package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 3D space.
 *
 * @author Pim
 */
public final class Vector3Dd implements Cloneable
{
    private double x, y, z;

    public Vector3Dd(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3Dd(final @NotNull Vector3Dd other)
    {
        x = other.x;
        y = other.y;
        z = other.z;
    }

    public Vector3Dd add(final @NotNull Vector3Dd other)
    {
        add(other.x, other.y, other.z);
        return this;
    }

    public Vector3Dd subtract(final @NotNull Vector3Dd other)
    {
        add(-other.x, -other.y, -other.z);
        return this;
    }

    public Vector3Dd multiply(final @NotNull Vector3Dd other)
    {
        x *= other.x;
        y *= other.y;
        z *= other.z;
        return this;
    }

    public Vector3Dd divide(final @NotNull Vector3Dd other)
    {
        x /= other.x;
        y /= other.y;
        z /= other.z;
        return this;
    }

    public Vector3Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        return this;
    }

    public Vector3Dd divide(final double val)
    {
        x /= val;
        y /= val;
        y /= val;
        return this;
    }

    public Vector3Dd addX(double val)
    {
        x += val;
        return this;
    }

    public Vector3Dd addY(double val)
    {
        y += val;
        return this;
    }

    public Vector3Dd addZ(double val)
    {
        z += val;
        return this;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getZ()
    {
        return z;
    }

    public Vector3Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    public Vector3Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    public Vector3Dd setZ(double newVal)
    {
        z = newVal;
        return this;
    }

    public Vector3Dd add(double x, double y, double z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    @NotNull
    public Vector3Dd clone()
    {
        try
        {
            return (Vector3Dd) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }

    @Override
    public String toString()
    {
        return "(" + x + ":" + y + ":" + z + ")";
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 19 * hash + Double.valueOf(x).hashCode();
        hash = 19 * hash + Double.valueOf(y).hashCode();
        hash = 19 * hash + Double.valueOf(z).hashCode();
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
        Vector3Dd other = (Vector3Dd) o;
        return x == other.x && y == other.y && z == other.z;
    }
}
