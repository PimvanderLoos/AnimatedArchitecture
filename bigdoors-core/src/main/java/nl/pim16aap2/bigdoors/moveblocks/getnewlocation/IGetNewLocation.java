package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface IGetNewLocation
{
    @NotNull
    Location getNewLocation(final double radius, final double xPos, final double yPos, final double zPos);
}
