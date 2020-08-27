package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an int vector or vertex in 4D space.
 *
 * @author Pim
 */
@Data
@AllArgsConstructor
public final class Vector4Di implements IVector4DiConst, Cloneable
{
    @Getter
    private int x, y, z, w;

    public Vector4Di(final @NotNull IVector4DiConst other)
    {
        x = other.getX();
        y = other.getY();
        z = other.getZ();
        w = other.getW();
    }

    @NotNull
    public Vector4Di add(final @NotNull IVector4DiConst other)
    {
        add(other.getX(), other.getY(), other.getZ(), other.getW());
        return this;
    }

    @NotNull
    public Vector4Di subtract(final @NotNull IVector4DiConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ(), -other.getW());
        return this;
    }

    @NotNull
    public Vector4Di multiply(final @NotNull IVector4DiConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getY();
        w *= other.getY();
        return this;
    }

    @NotNull
    public Vector4Di divide(final @NotNull IVector4DiConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getY();
        w /= other.getY();
        return this;
    }

    @NotNull
    public Vector4Di multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        w *= val;
        return this;
    }

    @NotNull
    public Vector4Di divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        w /= val;
        return this;
    }

    @NotNull
    public Vector4Di addX(int val)
    {
        x += val;
        return this;
    }

    @NotNull
    public Vector4Di addY(int val)
    {
        y += val;
        return this;
    }

    @NotNull
    public Vector4Di addZ(int val)
    {
        z += val;
        return this;
    }

    @NotNull
    public Vector4Di addW(int val)
    {
        w += val;
        return this;
    }

    @NotNull
    public Vector4Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    @NotNull
    public Vector4Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    @NotNull
    public Vector4Di setZ(int newVal)
    {
        z = newVal;
        return this;
    }

    @NotNull
    public Vector4Di setW(int newVal)
    {
        w = newVal;
        return this;
    }

    @NotNull
    public Vector4Di add(int x, int y, int z, int w)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    @Override
    public @NotNull Vector4Di clone()
    {
        try
        {
            return (Vector4Di) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // TODO: Only log to file! It's already dumped in the console because it's thrown.
            Error er = new Error(e);
            PLogger.get().logThrowable(er);
            throw er;
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
        return x == other.getX() && y == other.getY() && z == other.getZ() && w == other.getW();
    }

    public @NotNull Vector4Di normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);

        x /= length;
        y /= length;
        z /= length;
        w /= length;

        return this;
    }
}
