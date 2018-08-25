package nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

@SuppressWarnings("unused")
public class GetNewLocationSouth implements GetNewLocation
{
	private World                 world;
	private RotateDirection      upDown;
	private DoorDirection openDirection;
	private int        xMin, yMin, zMin;
	private int        xMax, yMax, zMax;
	
	public GetNewLocationSouth(World world, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax, RotateDirection upDown, DoorDirection openDirection)
	{
		this.openDirection = openDirection;
		this.upDown        = upDown;
		this.world         = world;
		this.xMin          = xMin;
		this.xMax          = xMax;
		this.yMin          = yMin;
		this.yMax          = yMax;
		this.zMin          = zMin;
		this.zMax          = zMax;
	}

	public GetNewLocationSouth()
	{}
	
	@Override
	public Location getNewLocation(double radius, double xPos, double yPos, double zPos, int index)
	{
		Location newPos = null;

		if (upDown == RotateDirection.UP)
			newPos = new Location(world, xPos, yMin + radius, this.zMax);
		else if (openDirection.equals(DoorDirection.NORTH))
			newPos = new Location(world, xPos, yMin, zPos - radius);
		else if (openDirection.equals(DoorDirection.SOUTH))
			newPos = new Location(world, xPos, yMin, zPos + radius);
		return newPos;
	}
}
