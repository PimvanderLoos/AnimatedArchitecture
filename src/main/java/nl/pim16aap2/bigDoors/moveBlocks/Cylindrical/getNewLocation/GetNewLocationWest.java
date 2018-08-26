package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.util.MyBlockData;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class GetNewLocationWest implements GetNewLocation
{
	@SuppressWarnings("unused")
	private int xMin, xMax, zMin, zMax;
	private World world;
	private RotateDirection rotDir;
	
	public GetNewLocationWest(World world, int xMin, int xMax, int zMin, int zMax, RotateDirection rotDir)
	{
		this.rotDir = rotDir;
		this.world  = world;
		this.xMin   = xMin;
		this.xMax   = xMax;
		this.zMin   = zMin;
		this.zMax   = zMax;
	}

	public GetNewLocationWest()
	{}

	@Override
	public Location getNewLocation(List<MyBlockData> savedBlocks, double xPos, double yPos, double zPos, int index)
	{
		Location oldPos = new Location(world, xPos, yPos, zPos);
		Location newPos = oldPos;

		double radius = savedBlocks.get(index).getRadius();

		newPos.setX(xMax);
		newPos.setY(oldPos.getY());
		newPos.setZ(oldPos.getZ() + (rotDir == RotateDirection.CLOCKWISE ? -radius : radius));
		return newPos;
	}
}
