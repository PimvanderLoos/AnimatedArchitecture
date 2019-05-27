package nl.pim16aap2.bigdoors.moveblocks.getnewlocation;

import org.bukkit.Location;

public interface GetNewLocation
{
    public Location getNewLocation(double radius, double xPos, double yPos, double zPos);
}