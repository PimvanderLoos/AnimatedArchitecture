package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import nl.pim16aap2.bigdoors.api.IPLocation;
import org.jetbrains.annotations.NotNull;

public interface IGetNewLocation
{
    @NotNull IPLocation getNewLocation(double radius, double xPos, double yPos, double zPos);
}
