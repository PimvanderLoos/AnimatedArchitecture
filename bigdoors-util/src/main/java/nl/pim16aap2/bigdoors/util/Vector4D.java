package nl.pim16aap2.bigdoors.util;

/**
 * Represents an Integer vector or vertex in 4D space.
 *
 * @autor Pim
 */
public final class Vector4D implements Cloneable
{
    private int x, y, z, w;

    public Vector4D(int x, int y, int z, int w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4D(Vector4D other)
    {
        x = other.x;
        y = other.y;
        z = other.z;
        w = other.w;
    }

    public void add(Vector4D other)
    {
        add(other.x, other.y, other.z, other.w);
    }

    public void subtract(Vector4D other)
    {
        add(-other.x, -other.y, -other.z, -other.w);
    }

    public void addX(int val)
    {
        x += val;
    }

    public void addY(int val)
    {
        y += val;
    }

    public void addZ(int val)
    {
        z += val;
    }

    public void addW(int val)
    {
        w += val;
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

    public void add(int x, int y, int z, int w)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
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
        if (this.getClass() != o.getClass())
            return false;
        Vector4D other = (Vector4D) o;
        return x == other.x && y == other.y && z == other.z && w == other.w;
    }
}
