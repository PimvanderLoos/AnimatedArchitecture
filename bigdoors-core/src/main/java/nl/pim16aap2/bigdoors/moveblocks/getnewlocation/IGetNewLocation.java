package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPLocation;

public interface IGetNewLocation
{
    @NonNull IPLocation getNewLocation(final double radius, final double xPos, final double yPos, final double zPos);
}
