package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an int vector or vertex in 3D space.
 *
 * @author Pim
 */
@Data
@AllArgsConstructor
public final class Vector3Di implements IVector3DiConst, Cloneable
{
    @Getter
    private int x, y, z;

    public Vector3Di(final @NotNull IVector3DiConst other)
    {
        x = other.getX();
        y = other.getY();
        z = other.getZ();
    }

    // TODO: Test this.
    public Vector3Di rotateAroundXAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newY = cos * (double) getY() - sin * (double) getZ();
        double newZ = sin * (double) getY() + cos * (double) getZ();

        setY((int) newY);
        setZ((int) newZ);
        return this;
    }

    // TODO: Test this.
    public Vector3Di rotateAroundXAxis(final @NotNull IVector3DiConst pivotPoint, final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double translatedY = y - pivotPoint.getY();
        double translatedZ = z - pivotPoint.getZ();

        double changeY = cos * translatedY - sin * translatedZ;
        double changeZ = sin * translatedY + cos * translatedZ;

        setY((int) (pivotPoint.getY() + changeY));
        setZ((int) (pivotPoint.getZ() + changeZ));
        return this;
    }

    // TODO: Test this.
    public Vector3Di rotateAroundYAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newX = cos * (double) getX() + sin * (double) getZ();
        double newZ = -sin * (double) getX() + cos * (double) getZ();

        setX((int) newX);
        setZ((int) newZ);
        return this;
    }

    public Vector3Di rotateAroundYAxis(final @NotNull IVector3DiConst pivotPoint, final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double translatedX = x - pivotPoint.getX();
        double translatedZ = z - pivotPoint.getZ();

        double changeX = cos * translatedX - sin * translatedZ;
        double changeZ = sin * translatedX + cos * translatedZ;

        setX((int) (pivotPoint.getX() + changeX));
        setZ((int) (pivotPoint.getZ() + changeZ));
        return this;
    }

    // TODO: Test this.
    public Vector3Di rotateAroundZAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newX = cos * (double) getX() - sin * (double) getY();
        double newY = sin * (double) getX() + cos * (double) getY();

        setX((int) newX);
        setY((int) newY);

        return this;
    }

    // TODO: Test this.
    public Vector3Di rotateAroundZAxis(final @NotNull IVector3DiConst pivotPoint, final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double translatedX = x - pivotPoint.getX();
        double translatedY = y - pivotPoint.getY();

        double changeX = cos * translatedX - sin * translatedY;
        double changeY = sin * translatedX + cos * translatedY;

        setX((int) (pivotPoint.getX() + changeX));
        setY((int) (pivotPoint.getZ() + changeY));
        return this;
    }

    public Vector3Di add(final @NotNull IVector3DiConst other)
    {
        add(other.getX(), other.getY(), other.getZ());
        return this;
    }

    public Vector3Di subtract(final @NotNull IVector3DiConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ());
        return this;
    }

    public Vector3Di multiply(final @NotNull IVector3DiConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getZ();
        return this;
    }

    public Vector3Di divide(final @NotNull IVector3DiConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getZ();
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
        return x == other.getX() && y == other.getY() && z == other.getZ();
    }

    public Vector3Di normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z);

        x /= length;
        y /= length;
        z /= length;

        return this;
    }
}
