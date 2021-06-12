package nl.pim16aap2.bigDoors.util;

public class Vector3D
{
    private int x, y, z;

    public Vector3D(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D(Vector3D other)
    {
        x = other.x;
        y = other.y;
        z = other.z;
    }

    public void add(Vector3D other)
    {
        add(other.x, other.y, other.z);
    }

    public void subtract(Vector3D other)
    {
        add(-other.x, -other.y, -other.z);
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

    public void add(int x, int y, int z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        Vector3D other = (Vector3D) o;
        return x == other.x && y == other.y && z == other.z;
    }
}