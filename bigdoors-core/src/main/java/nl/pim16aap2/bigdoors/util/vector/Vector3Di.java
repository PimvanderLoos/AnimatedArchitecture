package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an int vector or vertex in 3D space.
 *
 * @author Pim
 */
public final class Vector3Di implements Cloneable
{
    private int x, y, z;

    public Vector3Di(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3Di(final @NotNull Vector3Di other)
    {
        x = other.x;
        y = other.y;
        z = other.z;
    }

    public Vector3Di add(final @NotNull Vector3Di other)
    {
        add(other.x, other.y, other.z);
        return this;
    }

    public Vector3Di subtract(final @NotNull Vector3Di other)
    {
        add(-other.x, -other.y, -other.z);
        return this;
    }

    public Vector3Di multiply(final @NotNull Vector3Di other)
    {
        x *= other.x;
        y *= other.y;
        z *= other.z;
        return this;
    }

    public Vector3Di divide(final @NotNull Vector3Di other)
    {
        x /= other.x;
        y /= other.y;
        z /= other.z;
        return this;
    }

    public Vector3Di multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        return this;
    }

    public Vector3Di divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        return this;
    }

    public Vector3Di addX(int val)
    {
        x += val;
        return this;
    }

    public Vector3Di addY(int val)
    {
        y += val;
        return this;
    }

    public Vector3Di addZ(int val)
    {
        z += val;
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

    public Vector3Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public Vector3Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public Vector3Di setZ(int newVal)
    {
        z = newVal;
        return this;
    }

    public Vector3Di add(int x, int y, int z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    @NotNull
    public Vector3Di clone()
    {
        try
        {
            return (Vector3Di) super.clone();
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
        hash = 19 * hash + x;
        hash = 19 * hash + y;
        hash = 19 * hash + z;
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
        Vector3Di other = (Vector3Di) o;
        return x == other.x && y == other.y && z == other.z;
    }
}
