package nl.pim16aap2.bigdoors.util.vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class Vector3DdConst
{
    @Getter
    protected double x, y, z;

    public Vector3DdConst(final @NotNull Vector3DdConst other)
    {
        this(other.getX(), other.getY(), other.getZ());
    }

    public Vector3DdConst(final @NotNull Vector3DiConst other)
    {
        this(other.getX(), other.getY(), other.getZ());
    }

    public @NotNull IPLocation toLocation(final @NotNull IPWorld world)
    {
        return BigDoors.get().getPlatform().getPLocationFactory().create(world, this);
    }

    public double getDistance(final @NotNull Vector3DiConst point)
    {
        return Math.sqrt(Math.pow(getX() - point.getX(), 2) + Math.pow(getY() - point.getY(), 2) +
                             Math.pow(getZ() - point.getZ(), 2));
    }

    public double getDistance(final @NotNull Vector3DdConst point)
    {
        return Math.sqrt(Math.pow(getX() - point.getX(), 2) + Math.pow(getY() - point.getY(), 2) +
                             Math.pow(getZ() - point.getZ(), 2));
    }

    /**
     * Converts this object to a String to a certain number of decimal places.
     *
     * @param decimals The number of digits after the dot to display.
     * @return The String representing this object.
     */
    public @NotNull String toString(final int decimals)
    {
        final @NotNull String placeholder = "%." + decimals + "f";
        return String.format(placeholder + ", " + placeholder + ", " + placeholder, x, y, z);
    }

    @Override
    public @NotNull String toString()
    {
        return "(" + x + ":" + y + ":" + z + ")";
    }

    @Override
    public @NotNull Vector3Dd clone()
    {
        return new Vector3Dd(this);
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof Vector3DdConst))
            return false;

        Vector3DdConst other = (Vector3DdConst) o;
        return x == other.getX() && y == other.getY() && z == other.getZ();
    }
}
