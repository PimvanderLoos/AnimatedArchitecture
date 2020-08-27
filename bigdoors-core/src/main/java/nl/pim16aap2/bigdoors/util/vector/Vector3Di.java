package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.PLogger;
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
    @NotNull
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

    @NotNull
    public Vector3Di rotateAroundXAxis(final @NotNull IVector3DiConst pivotPoint, final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedY = y - pivotPoint.getY();
        final double translatedZ = z - pivotPoint.getZ();

        final double changeY = cos * translatedY - sin * translatedZ;
        final double changeZ = sin * translatedY + cos * translatedZ;

        setY((int) (pivotPoint.getY() + changeY));
        setZ((int) (pivotPoint.getZ() + changeZ));
        return this;
    }

    // TODO: Test this.
    @NotNull
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

    @NotNull
    public Vector3Di rotateAroundYAxis(final @NotNull IVector3DiConst pivotPoint, final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedX = x - pivotPoint.getX();
        final double translatedZ = z - pivotPoint.getZ();

        final double changeX = cos * translatedX - sin * translatedZ;
        final double changeZ = sin * translatedX + cos * translatedZ;

        setX((int) (pivotPoint.getX() + changeX));
        setZ((int) (pivotPoint.getZ() + changeZ));
        return this;
    }

    // TODO: Test this.
    @NotNull
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

    @NotNull
    public Vector3Di rotateAroundZAxis(final @NotNull IVector3DiConst pivotPoint, final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedY = y - pivotPoint.getY();
        final double translatedX = x - pivotPoint.getX();

        final double changeY = cos * translatedY - sin * translatedX;
        final double changeX = sin * translatedY + cos * translatedX;

        setY((int) (pivotPoint.getY() + changeY));
        setX((int) (pivotPoint.getX() + changeX));
        return this;
    }

    @NotNull
    public Vector3Di add(final @NotNull IVector3DiConst other)
    {
        add(other.getX(), other.getY(), other.getZ());
        return this;
    }

    @NotNull
    public Vector3Di subtract(final @NotNull IVector3DiConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ());
        return this;
    }

    @NotNull
    public Vector3Di multiply(final @NotNull IVector3DiConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getZ();
        return this;
    }

    @NotNull
    public Vector3Di divide(final @NotNull IVector3DiConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getZ();
        return this;
    }

    @NotNull
    public Vector3Di multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        return this;
    }

    @NotNull
    public Vector3Di divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        return this;
    }

    @NotNull
    public Vector3Di addX(int val)
    {
        x += val;
        return this;
    }

    @NotNull
    public Vector3Di addY(int val)
    {
        y += val;
        return this;
    }

    @NotNull
    public Vector3Di addZ(int val)
    {
        z += val;
        return this;
    }

    @NotNull
    public Vector3Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    @NotNull
    public Vector3Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    @NotNull
    public Vector3Di setZ(int newVal)
    {
        z = newVal;
        return this;
    }

    @NotNull
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
            // TODO: Only log to file! It's already dumped in the console because it's thrown.
            Error er = new Error(e);
            PLogger.get().logError(er);
            throw er;
        }
    }

    @Override
    public IPLocation toLocation(final @NotNull IPWorld world)
    {
        System.out.println("Creating Location from vector: " + toString());
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

    @NotNull
    public Vector3Di normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z);

        x /= length;
        y /= length;
        z /= length;

        return this;
    }
}
