package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation;

import org.bukkit.Location;

public interface GetNewLocation
{
    public Location getNewLocation(double radius, double xPos, double yPos, double zPos);
}