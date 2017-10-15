package nl.pim16aap2.bigDoors.moveBlocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class DoorOpener
{
	private BigDoors plugin;
	
	DoorDirection ddirection;

	public DoorOpener(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	// Check if the block on the north/east/south/west side of the location if free or not.
	public boolean isPosFree(Location loc, DoorDirection direction)
	{
		Location newLoc = loc;
		switch(direction)
		{
		case NORTH:
			newLoc.setZ(newLoc.getZ() - 1);
			break;
		case EAST:
			newLoc.setX(newLoc.getX() + 1);
			break;
		case SOUTH:
			newLoc.setZ(newLoc.getZ() + 1);
			break;
		case WEST:
			newLoc.setX(newLoc.getX() - 1);
			break;
		}
		return newLoc.getWorld().getBlockAt(newLoc).getType() == Material.AIR;
	}
	
	// Determine which direction the door is going to rotate. Clockwise or counterclockwise.
	public RotateDirection getRotationDirection(Door door, DoorDirection currentDir)
	{
		switch(currentDir)
		{		
		case NORTH:
			if (isPosFree(door.getEngine(), DoorDirection.EAST))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door.getEngine(), DoorDirection.WEST))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
			
		case EAST:
			if (isPosFree(door.getEngine(), DoorDirection.SOUTH))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door.getEngine(), DoorDirection.NORTH))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
			
		case SOUTH:
			if (isPosFree(door.getEngine(), DoorDirection.WEST))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door.getEngine(), DoorDirection.EAST))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
			
		case WEST:
			if (isPosFree(door.getEngine(), DoorDirection.NORTH))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door.getEngine(), DoorDirection.SOUTH))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
		}
		return null;
	}
	
	// Get the direction the door is currently facing as seen from the engine to the end of the door.
	public DoorDirection getCurrentDirection(Door door)
	{
		// MinZ != EngineZ => North
		// MaxX != EngineX => East
		// MaxZ != EngineZ => South
		// MinX != EngineX => West
		return 	door.getEngine().getBlockZ() != door.getMinimum().getBlockZ() ? DoorDirection.NORTH :
				door.getEngine().getBlockX() != door.getMaximum().getBlockX() ? DoorDirection.EAST  :
				door.getEngine().getBlockZ() != door.getMaximum().getBlockZ() ? DoorDirection.SOUTH :
				door.getEngine().getBlockX() != door.getMinimum().getBlockX() ? DoorDirection.WEST  : null;
	}
	
	
	// Open a door.
	public boolean openDoor(Door door, double speed)
	{
		DoorDirection currentDirection = getCurrentDirection(door);
		if (currentDirection == null)
		{
			Bukkit.broadcastMessage("Current direction is null!");
			return false;
		}
		RotateDirection rotDirection   = getRotationDirection(door, currentDirection);
		if (rotDirection == null)
		{
			Bukkit.broadcastMessage("RotDirection is null!");
			return false;
		}
		
		Bukkit.broadcastMessage("CurrentDirection = " + currentDirection + ", performing " + rotDirection + " rotation.");
		
		int xOpposite, yOpposite, zOpposite;
		// If the xMax is not the same value as the engineX, then xMax is xOpposite.
		if (door.getMaximum().getBlockX() != door.getEngine().getBlockX())
		{
			xOpposite = door.getMaximum().getBlockX();
		} else
		{
			xOpposite = door.getMinimum().getBlockX();
		}
		// If the zMax is not the same value as the engineZ, then zMax is zOpposite.
		if (door.getMaximum().getBlockZ() != door.getEngine().getBlockZ())
		{
			zOpposite = door.getMaximum().getBlockZ();
		} else
		{
			zOpposite = door.getMinimum().getBlockZ();
		}
		// If the yMax is not the same value as the engineY, then yMax is yOpposite.
		if (door.getMaximum().getBlockY() != door.getEngine().getBlockY())
		{
			yOpposite = door.getMaximum().getBlockY();
		} else
		{
			yOpposite = door.getMinimum().getBlockY();
		}
		
		// Finalise the oppositePoint location.
		Location oppositePoint = new Location(door.getWorld(), xOpposite, yOpposite, zOpposite);
		
		// Make a new blockMover object and give it the variables required for the animation.
		BlockMover blockMover = new BlockMover(plugin, speed);
		blockMover.moveBlocks(door.getEngine(), oppositePoint, rotDirection, currentDirection);

		// Tell the door object it has been opened and what its new coordinates are.
		toggleOpen(door);
		updateCoords(door, currentDirection, rotDirection);

		return true;
	}
	
	public void updateCoords(Door door, DoorDirection currentDirection, RotateDirection rotDirection)
	{
		int xMin = door.getMinimum().getBlockX();
		int yMin = door.getMinimum().getBlockY();
		int zMin = door.getMinimum().getBlockZ();
		int xMax = door.getMaximum().getBlockX();
		int yMax = door.getMaximum().getBlockY();
		int zMax = door.getMaximum().getBlockZ();
		int xLen = xMax - xMin;
		int zLen = zMax - zMin;
		Location newMax = null;
		Location newMin = null;

		switch (currentDirection)
		{
		case NORTH:
			if (rotDirection == RotateDirection.CLOCKWISE)
			{
				newMin = new Location(door.getWorld(), xMin, yMin, zMax);
				newMax = new Location(door.getWorld(), (xMin + zLen), yMax, zMax);
			} else
			{
				newMin = new Location(door.getWorld(), (xMin - zLen), yMin, zMax);
				newMax = new Location(door.getWorld(), xMax, yMax, zMax);
			}
			break;
			
			
		case EAST:
			if (rotDirection == RotateDirection.CLOCKWISE)
			{
				newMin = new Location(door.getWorld(), xMin, yMin, zMin);
				newMax = new Location(door.getWorld(), xMin, yMax, (zMax + xLen));
			} else
			{
				newMin = new Location(door.getWorld(), xMin, yMin, (zMin - xLen));
				newMax = new Location(door.getWorld(), xMin, yMax, zMin);
			}
			break;
			
			
		case SOUTH:
			if (rotDirection == RotateDirection.CLOCKWISE)
			{
				newMin = new Location(door.getWorld(), (xMin - zLen), yMin, zMin);
				newMax = new Location(door.getWorld(), xMax, yMax, zMin);
			} else
			{
				newMin = new Location(door.getWorld(), xMin, yMin, zMin);
				newMax = new Location(door.getWorld(), (xMin + zLen), yMax, zMin);
			}
			break;
			
			
		case WEST:
			if (rotDirection == RotateDirection.CLOCKWISE)
			{
				newMin = new Location(door.getWorld(), xMax, yMin, (zMin - xLen));
				newMax = new Location(door.getWorld(), xMax, yMax, zMax);
			} else
			{
				newMin = new Location(door.getWorld(), xMax, yMin, zMin);
				newMax = new Location(door.getWorld(), xMax, yMax, (zMax + xLen));
			}
			break;
		}
		door.setMaximum(newMax);
		door.setMinimum(newMin);
	}

	public void toggleOpen(Door door)
	{
		door.setStatus(!door.getStatus());
	}
}
