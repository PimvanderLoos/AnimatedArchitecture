package nl.pim16aap2.bigdoors.util.vector;


import lombok.NonNull;

/**
 * Represents a double vector or vertex in 3D space.
 *
 * @author Pim
 */
public final class Vector3Dd extends Vector3DdConst
{
    public Vector3Dd(final double x, final double y, final double z)
    {
        super(x, y, z);
    }

    public Vector3Dd(final @NonNull Vector3DdConst other)
    {
        super(other);
    }

    public Vector3Dd(final @NonNull Vector3DiConst other)
    {
        super(other);
    }

    public @NonNull Vector3Dd rotateAroundXAxis(final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double newY = cos * getY() - sin * getZ();
        final double newZ = sin * getY() + cos * getZ();

        setY(newY);
        setZ(newZ);

        return this;
    }

    public @NonNull Vector3Dd rotateAroundXAxis(final @NonNull Vector3DdConst pivotPoint, final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedY = y - pivotPoint.getY();
        final double translatedZ = z - pivotPoint.getZ();

        final double changeY = cos * translatedY - sin * translatedZ;
        final double changeZ = sin * translatedY + cos * translatedZ;

        setY(pivotPoint.getY() + changeY);
        setZ(pivotPoint.getZ() + changeZ);
        return this;
    }

    public @NonNull Vector3Dd rotateAroundYAxis(final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double newX = cos * getX() + sin * getZ();
        final double newZ = -sin * getX() + cos * getZ();

        setX(newX);
        setZ(newZ);

        return this;
    }

    public @NonNull Vector3Dd rotateAroundYAxis(final @NonNull Vector3DdConst pivotPoint, final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedX = x - pivotPoint.getX();
        final double translatedZ = z - pivotPoint.getZ();

        final double changeX = cos * translatedX - sin * translatedZ;
        final double changeZ = sin * translatedX + cos * translatedZ;

        setX(pivotPoint.getX() + changeX);
        setZ(pivotPoint.getZ() + changeZ);
        return this;
    }

    public @NonNull Vector3Dd rotateAroundZAxis(final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double newX = cos * getX() - sin * getY();
        final double newY = sin * getX() + cos * getY();

        setX(newX);
        setY(newY);

        return this;
    }

    public @NonNull Vector3Dd rotateAroundZAxis(final @NonNull Vector3DdConst pivotPoint, final double radians)
    {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double translatedY = y - pivotPoint.getY();
        final double translatedX = x - pivotPoint.getX();

        final double changeY = cos * translatedY - sin * translatedX;
        final double changeX = sin * translatedY + cos * translatedX;

        setY(pivotPoint.getY() + changeY);
        setX(pivotPoint.getX() + changeX);
        return this;
    }

    public @NonNull Vector3Dd add(final @NonNull Vector3DdConst other)
    {
        add(other.getX(), other.getY(), other.getZ());
        return this;
    }

    public @NonNull Vector3Dd subtract(final @NonNull Vector3DdConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ());
        return this;
    }

    public @NonNull Vector3Dd multiply(final @NonNull Vector3DdConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getZ();
        return this;
    }

    public @NonNull Vector3Dd divide(final @NonNull Vector3DdConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getZ();
        return this;
    }

    public @NonNull Vector3Dd multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        return this;
    }

    public @NonNull Vector3Dd divide(final double val)
    {
        x /= val;
        y /= val;
        y /= val;
        return this;
    }

    public @NonNull Vector3Dd addX(double val)
    {
        x += val;
        return this;
    }

    public @NonNull Vector3Dd addY(double val)
    {
        y += val;
        return this;
    }

    public @NonNull Vector3Dd addZ(double val)
    {
        z += val;
        return this;
    }

    public @NonNull Vector3Dd setX(double newVal)
    {
        x = newVal;
        return this;
    }

    public @NonNull Vector3Dd setY(double newVal)
    {
        y = newVal;
        return this;
    }

    public @NonNull Vector3Dd setZ(double newVal)
    {
        z = newVal;
        return this;
    }

    public @NonNull Vector3Dd add(double x, double y, double z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    public @NonNull Vector3Dd clone()
    {
        return new Vector3Dd(this);
    }

    public @NonNull Vector3Dd normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z);

        x /= length;
        y /= length;
        z /= length;

        return this;
    }
}
