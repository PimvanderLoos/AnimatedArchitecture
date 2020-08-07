package nl.pim16aap2.bigdoors.util.vector;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import org.jetbrains.annotations.NotNull;

public interface IVector3DiConst
{
    int getX();

    int getY();

    int getZ();

    IPLocation toLocation(final @NotNull IPWorld world);
}
