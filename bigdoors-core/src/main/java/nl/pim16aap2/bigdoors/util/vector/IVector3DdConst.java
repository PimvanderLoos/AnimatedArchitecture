package nl.pim16aap2.bigdoors.util.vector;

import org.jetbrains.annotations.NotNull;

public interface IVector3DdConst
{
    double getX();

    double getY();

    double getZ();

    default double getDistance(final @NotNull IVector3DiConst point)
    {
        return Math.sqrt(Math.pow(getX() - point.getX(), 2) + Math.pow(getY() - point.getY(), 2) +
                             Math.pow(getZ() - point.getZ(), 2));
    }
}
