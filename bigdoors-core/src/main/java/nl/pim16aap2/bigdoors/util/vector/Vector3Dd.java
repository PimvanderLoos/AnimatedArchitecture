package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 3D space.
 *
 * @author Pim
 */
@Data
@AllArgsConstructor
// TODO: Write test.
public final class Vector3Dd implements IVector3DdConst, Cloneable
{
    @Getter
    private double x, y, z;

    public Vector3Dd(final @NotNull IVector3DdConst other)
    {
        x = other.getX();
        y = other.getY();
        z = other.getZ();
    }

    public Vector3Dd rotateAroundXAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newY = cos * getY() - sin * getZ();
        double newZ = sin * getY() + cos * getZ();

        setY(newY);
        setZ(newZ);

        return this;
    }

    public Vector3Dd rotateAroundXAxis(final @NotNull IVector3DdConst pivotPoint, final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double translatedY = y - pivotPoint.getY();
        double translatedZ = z - pivotPoint.getZ();

        double changeY = cos * translatedY - sin * translatedZ;
        double changeZ = sin * translatedY + cos * translatedZ;

        setY(pivotPoint.getY() + changeY);
        setZ(pivotPoint.getZ() + changeZ);
        return this;
    }

    public Vector3Dd rotateAroundYAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newX = cos * getX() + sin * getZ();
        double newZ = -sin * getX() + cos * getZ();

        setX(newX);
        setZ(newZ);

        return this;
    }

    public Vector3Dd rotateAroundYAxis(final @NotNull IVector3DdConst pivotPoint, final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double translatedX = x - pivotPoint.getX();
        double translatedZ = z - pivotPoint.getZ();

        double changeX = cos * translatedX - sin * translatedZ;
        double changeZ = sin * translatedX + cos * translatedZ;

        setX(pivotPoint.getX() + changeX);
        setZ(pivotPoint.getZ() + changeZ);
        return this;
    }

    public Vector3Dd rotateAroundZAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newX = cos * getX() - sin * getY();
        double newY = sin * getX() + cos * getY();

        setX(newX);
        setY(newY);

        return this;
    }

    public Vector3Dd rotateAroundZAxis(final @NotNull IVector3DdConst pivotPoint, final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double translatedX = x - pivotPoint.getX();
        double translatedY = y - pivotPoint.getY();

        double changeX = cos * translatedX - sin * translatedY;
        double changeY = sin * translatedX + cos * translatedY;

        setX(pivotPoint.getX() + changeX);
        setY(pivotPoint.getZ() + changeY);
        return this;
    }

    public Vector3Dd add(final @NotNull IVector3DdConst other)
    {
        add(other.getX(), other.getY(), other.getZ());
        return this;
    }

    public Vector3Dd subtract(final @NotNull IVector3DdConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ());
        return this;
    }

    public Vector3Dd multiply(final @NotNull IVector3DdConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getZ();
        return this;
    }

    public Vector3Dd divide(final @NotNull IVector3DdConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getZ();
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
    public IPLocation toLocation(final @NotNull IPWorld world)
    {
        return BigDoors.get().getPlatform().getPLocationFactory().create(world, this);
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

    public Vector3Dd normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z);

        x /= length;
        y /= length;
        z /= length;

        return this;
    }
}
