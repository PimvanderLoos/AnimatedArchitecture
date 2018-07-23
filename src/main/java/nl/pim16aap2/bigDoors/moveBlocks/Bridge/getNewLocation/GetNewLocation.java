package nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation;

import java.util.List;

import org.bukkit.Location;

import nl.pim16aap2.bigDoors.util.BlockData;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public interface GetNewLocation
{
	public Location getNewLocation(double radius, double xPos, double yPos, double zPos, int index);
}