package nl.pim16aap2.bigdoors.util.vector;


import org.jetbrains.annotations.NotNull;

/**
 * Represents an integer vector or vertex in 3D space.
 *
 * @author Pim
 */
public final class Vector3Di extends Vector3DiConst
{
    public Vector3Di(final int x, final int y, final int z)
    {
        super(x, y, z);
    }

    public Vector3Di(final @NotNull Vector3DiConst other)
    {
        super(other);
    }

    // TODO: Test this.
    public @NotNull Vector3Di rotateAroundXAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newY = cos * (double) getY() - sin * (double) getZ();
        double newZ = sin * (double) getY() + cos * (double) getZ();

        setY((int) newY);
        setZ((int) newZ);
        return this;
    }

    public @NotNull Vector3Di rotateAroundXAxis(final @NotNull Vector3DiConst pivotPoint, final double radians)
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
    public @NotNull Vector3Di rotateAroundYAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newX = cos * (double) getX() + sin * (double) getZ();
        double newZ = -sin * (double) getX() + cos * (double) getZ();

        setX((int) newX);
        setZ((int) newZ);
        return this;
    }

    public @NotNull Vector3Di rotateAroundYAxis(final @NotNull Vector3DiConst pivotPoint, final double radians)
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
    public @NotNull Vector3Di rotateAroundZAxis(final double radians)
    {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double newX = cos * (double) getX() - sin * (double) getY();
        double newY = sin * (double) getX() + cos * (double) getY();

        setX((int) newX);
        setY((int) newY);

        return this;
    }

    public @NotNull Vector3Di rotateAroundZAxis(final @NotNull Vector3DiConst pivotPoint, final double radians)
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

    public @NotNull Vector3Di add(final @NotNull Vector3DiConst other)
    {
        add(other.getX(), other.getY(), other.getZ());
        return this;
    }

    public @NotNull Vector3Di subtract(final @NotNull Vector3DiConst other)
    {
        add(-other.getX(), -other.getY(), -other.getZ());
        return this;
    }

    public @NotNull Vector3Di multiply(final @NotNull Vector3DiConst other)
    {
        x *= other.getX();
        y *= other.getY();
        z *= other.getZ();
        return this;
    }

    public @NotNull Vector3Di divide(final @NotNull Vector3DiConst other)
    {
        x /= other.getX();
        y /= other.getY();
        z /= other.getZ();
        return this;
    }

    public @NotNull Vector3Di multiply(final double val)
    {
        x *= val;
        y *= val;
        z *= val;
        return this;
    }

    public @NotNull Vector3Di divide(final double val)
    {
        x /= val;
        y /= val;
        z /= val;
        return this;
    }

    public @NotNull Vector3Di addX(int val)
    {
        x += val;
        return this;
    }

    public @NotNull Vector3Di addY(int val)
    {
        y += val;
        return this;
    }

    public @NotNull Vector3Di addZ(int val)
    {
        z += val;
        return this;
    }

    public @NotNull Vector3Di setX(int newVal)
    {
        x = newVal;
        return this;
    }

    public @NotNull Vector3Di setY(int newVal)
    {
        y = newVal;
        return this;
    }

    public @NotNull Vector3Di setZ(int newVal)
    {
        z = newVal;
        return this;
    }

    public @NotNull Vector3Di add(int x, int y, int z)
    {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    public @NotNull Vector3Di clone()
    {
        return new Vector3Di(this);
    }

    public @NotNull Vector3Di normalize()
    {
        double length = Math.sqrt(x * x + y * y + z * z);

        x /= length;
        y /= length;
        z /= length;

        return this;
    }
}
