package nl.pim16aap2.bigdoors.util.vector;


import lombok.NonNull;

/**
 * Represents an int vector or vertex in 4D space.
 *
 * @author Pim
 */
public final class Vector4Di extends Vector4DiConst
{
    public Vector4Di(final int x, final int y, final int z, final int w)
    {
        super(x, y, z, w);
    }

    public Vector4Di(final @NonNull Vector4DiConst other)
    {
        super(other);
    }

    public @NonNull Vector4Di add(final @NonNull Vector4DiConst other)
    {
        add(other.getX(), other.getY(), other.getZ(), other.getW());
        return this;
    }

    public @NonNull Vector4Di subtract(final @NonNull Vector4DiConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ(), -other.getW());
        return this;
    }

    public @NonNull Vector4Di multiply(final @NonNull Vector4DiConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getY();
        w *= other.getY();
        return this;
    }

    public @NonNull Vector4Di divide(final @NonNull Vector4DiConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getY();
        w /= other.getY();
        return this;
    }

    public @NonNull Vector4Di multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        w *= val;
        return this;
    }

    public @NonNull Vector4Di divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        w /= val;
        return this;
    }

    public @NonNull Vector4Di addX(int val)
    {
        x += val;
        return this;
    }

    public @NonNull Vector4Di addY(int val)
    {
        y += val;
        return this;
    }

    public @NonNull Vector4Di addZ(int val)
    {
        z += val;
        return this;
    }

    public @NonNull Vector4Di addW(int val)
    {
        w += val;
        return this;
    }

    public @NonNull Vector4Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public @NonNull Vector4Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public @NonNull Vector4Di setZ(int newVal)
    {
        z = newVal;
        return this;
    }

    public @NonNull Vector4Di setW(int newVal)
    {
        w = newVal;
        return this;
    }

    public @NonNull Vector4Di add(int x, int y, int z, int w)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    @Override
    public @NonNull Vector4Di clone()
    {
        return new Vector4Di(this);
    }

    public @NonNull Vector4Di normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);

        x /= length;
        y /= length;
        z /= length;
        w /= length;

        return this;
    }
}
