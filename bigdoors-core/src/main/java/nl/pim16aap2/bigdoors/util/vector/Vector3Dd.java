package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a double vector or vertex in 3D space.
 *
 * @author Pim
 */
@Data
@AllArgsConstructor
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
