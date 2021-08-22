package nl.pim16aap2.bigDoors.util;

import net.minecraft.server.v1_11_R1.Vec3D;

public final class WorldHeightLimits
{
    private final int lowerLimit;
    private final int upperLimit;

    public WorldHeightLimits(final int lowerLimit, final int upperLimit)
    {
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
    }

    public int getLowerLimit()
    {
        return lowerLimit;
    }

    public int getUpperLimit()
    {
        return upperLimit;
    }

    @Override
    public String toString()
    {
        return "(lowerLimit: " + lowerLimit + ", upperLimit: " + upperLimit + ")";
    }

    @Override
    public boolean equals(final Object o)
    {
        if (o == this)
            return true;

        if (!(o instanceof WorldHeightLimits))
            return false;

        final WorldHeightLimits other = (WorldHeightLimits) o;

        return lowerLimit == other.lowerLimit && upperLimit == other.upperLimit;
    }

    @Override
    public int hashCode()
    {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.upperLimit;
        result = result * PRIME + this.lowerLimit;
        return result;
    }
}
