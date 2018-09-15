package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.Location;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class DoorOpener implements Opener
{
	private BigDoors plugin;
	
	DoorDirection ddirection;

	public DoorOpener(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	// Check if the block on the north/east/south/west side of the location is free.
	public boolean isPosFree(Door door, DoorDirection direction)
	{
		Location engLoc = door.getEngine();
		int endX   = 0, endY   = 0, endZ   = 0;
		int startX = 0, startY = 0, startZ = 0;
		int xLen = door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
		int zLen = door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();
		
		switch(direction)
		{
		case NORTH:
			startX = engLoc.getBlockX();
			startY = engLoc.getBlockY();
			startZ = engLoc.getBlockZ() - xLen;
			endX   = engLoc.getBlockX();
			endY   = door.getMaximum().getBlockY();
			endZ   = engLoc.getBlockZ() - 1;
			break;
		case EAST:
			startX = engLoc.getBlockX() + 1;
			startY = engLoc.getBlockY();
			startZ = engLoc.getBlockZ();
			endX   = engLoc.getBlockX() + zLen;
			endY   = door.getMaximum().getBlockY();
			endZ   = engLoc.getBlockZ();
			break;
		case SOUTH:
			startX = engLoc.getBlockX();
			startY = engLoc.getBlockY();
			startZ = engLoc.getBlockZ() + 1;
			endX   = engLoc.getBlockX();
			endY   = door.getMaximum().getBlockY();
			endZ   = engLoc.getBlockZ() + xLen;
			break;
		case WEST:
			startX = engLoc.getBlockX() - zLen;
			startY = engLoc.getBlockY();
			startZ = engLoc.getBlockZ();
			endX   = engLoc.getBlockX() - 1;
			endY   = door.getMaximum().getBlockY();
			endZ   = engLoc.getBlockZ();
			break;
		}

		for (int xAxis = startX; xAxis <= endX; ++xAxis)
			for (int yAxis = startY; yAxis <= endY; ++yAxis)
				for (int zAxis = startZ; zAxis <= endZ; ++zAxis)
					if (!Util.isAir(engLoc.getWorld().getBlockAt(xAxis, yAxis, zAxis).getType()))
						return false;
		return true;
	}
	
	// Determine which direction the door is going to rotate. Clockwise or counterclockwise.
	public RotateDirection getRotationDirection(Door door, DoorDirection currentDir)
	{
		switch(currentDir)
		{		
		case NORTH:
			if (isPosFree(door, DoorDirection.EAST))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door, DoorDirection.WEST))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
			
		case EAST:
			if (isPosFree(door, DoorDirection.SOUTH))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door, DoorDirection.NORTH))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
			
		case SOUTH:
			if (isPosFree(door, DoorDirection.WEST))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door, DoorDirection.EAST))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
			
		case WEST:
			if (isPosFree(door, DoorDirection.NORTH))
				return RotateDirection.CLOCKWISE;
			else if (isPosFree(door, DoorDirection.SOUTH))
				return RotateDirection.COUNTERCLOCKWISE;
			break;
		}
		return null;
	}
	
	// Get the direction the door is currently facing as seen from the engine to the end of the door.
	@Override
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
	
	// Check if the chunks at the minimum and maximum locations of the door are loaded.
	@Override
	public boolean chunksLoaded(Door door)
	{
		// Return true if the chunk at the max and at the min of the chunks were loaded correctly.
		if (door.getWorld() == null)
			plugin.getMyLogger().logMessage("World is null for door \""    + door.getName().toString() + "\"",          true, false);
		if (door.getWorld().getChunkAt(door.getMaximum()) == null)
			plugin.getMyLogger().logMessage("Chunk at maximum for door \"" + door.getName().toString() + "\" is null!", true, false);
		if (door.getWorld().getChunkAt(door.getMinimum()) == null)
			plugin.getMyLogger().logMessage("Chunk at minimum for door \"" + door.getName().toString() + "\" is null!", true, false);
		
		return door.getWorld().getChunkAt(door.getMaximum()).load() && door.getWorld().getChunkAt(door.getMinimum()).isLoaded();
	}
	
	@Override
	public int getDoorSize(Door door)
	{
		int xLen = Math.abs(door.getMaximum().getBlockX() - door.getMinimum().getBlockX());
		int yLen = Math.abs(door.getMaximum().getBlockY() - door.getMinimum().getBlockY());
		int zLen = Math.abs(door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ());
		xLen = xLen == 0 ? 1 : xLen;
		yLen = yLen == 0 ? 1 : yLen;
		zLen = zLen == 0 ? 1 : zLen;
		return xLen * yLen * zLen;
	}

	@Override
	public boolean openDoor(Door door, double time)
	{
		return openDoor(door, time, false, false);
	}
	
	// Open a door.
	@Override
	public boolean openDoor(Door door, double time, boolean instantOpen, boolean silent)
	{
		if (plugin.getCommander().isDoorBusy(door.getDoorUID()))
		{
			if (!silent)
				plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
			return true;
		}

		if (!chunksLoaded(door))
		{
			plugin.getMyLogger().logMessage(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!", true, false);
			return true;
		}

		DoorDirection currentDirection = getCurrentDirection(door);
		if (currentDirection == null)
		{
			plugin.getMyLogger().logMessage("Current direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!", true, false);
			return false;
		}

		RotateDirection rotDirection   = getRotationDirection(door, currentDirection);
		if (rotDirection == null)
		{
			plugin.getMyLogger().logMessage("Rotation direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!", true, false);
			return false;
		}
		
		int xOpposite, yOpposite, zOpposite;
		// If the xMax is not the same value as the engineX, then xMax is xOpposite.
		if (door.getMaximum().getBlockX() != door.getEngine().getBlockX())
			xOpposite = door.getMaximum().getBlockX();
		else
			xOpposite = door.getMinimum().getBlockX();

		// If the zMax is not the same value as the engineZ, then zMax is zOpposite.
		if (door.getMaximum().getBlockZ() != door.getEngine().getBlockZ())
			zOpposite = door.getMaximum().getBlockZ();
		else
			zOpposite = door.getMinimum().getBlockZ();

		// If the yMax is not the same value as the engineY, then yMax is yOpposite.
		if (door.getMaximum().getBlockY() != door.getEngine().getBlockY())
			yOpposite = door.getMaximum().getBlockY();
		else
			yOpposite = door.getMinimum().getBlockY();

		// Finalise the oppositePoint location.
		Location oppositePoint = new Location(door.getWorld(), xOpposite, yOpposite, zOpposite);
		
		// Make sure the doorSize does not exceed the total doorSize.
		// If it does, open the door instantly.
		int maxDoorSize = plugin.getConfigLoader().getInt("maxDoorSize");
		if (maxDoorSize != -1)
			if(getDoorSize(door) > maxDoorSize)
				instantOpen = true;
		
		// Change door availability so it cannot be opened again (just temporarily, don't worry!).
		plugin.getCommander().setDoorBusy(door.getDoorUID());
		
		new CylindricalMover(plugin, oppositePoint.getWorld(), 1, rotDirection, time, oppositePoint, currentDirection, door, instantOpen);

		// Tell the door object it has been opened and what its new coordinates are.
		toggleOpen  (door);
		updateCoords(door, currentDirection, rotDirection, -1);
		return true;
	}
	
	// Update the coordinates of a door based on its location, direction it's pointing in and rotation direction.
	@Override
	public void updateCoords(Door door, DoorDirection currentDirection, RotateDirection rotDirection, int moved)
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
				newMin = new Location(door.getWorld(), xMin,          yMin, zMax);
				newMax = new Location(door.getWorld(), (xMin + zLen), yMax, zMax);
			} 
			else
			{
				newMin = new Location(door.getWorld(), (xMin - zLen), yMin, zMax);
				newMax = new Location(door.getWorld(), xMax,          yMax, zMax);
			}
			break;
			
			
		case EAST:
			if (rotDirection == RotateDirection.CLOCKWISE)
			{
				newMin = new Location(door.getWorld(), xMin, yMin,          zMin);
				newMax = new Location(door.getWorld(), xMin, yMax, (zMax + xLen));
			} 
			else
			{
				newMin = new Location(door.getWorld(), xMin, yMin, (zMin - xLen));
				newMax = new Location(door.getWorld(), xMin, yMax,          zMin);
			}
			break;
			
			
		case SOUTH:
			if (rotDirection == RotateDirection.CLOCKWISE)
			{
				newMin = new Location(door.getWorld(), (xMin - zLen), yMin, zMin);
				newMax = new Location(door.getWorld(), xMax,          yMax, zMin);
			} 
			else
			{
				newMin = new Location(door.getWorld(), xMin,          yMin, zMin);
				newMax = new Location(door.getWorld(), (xMin + zLen), yMax, zMin);
			}
			break;
			
			
		case WEST:
			if (rotDirection == RotateDirection.CLOCKWISE)
			{
				newMin = new Location(door.getWorld(), xMax, yMin, (zMin - xLen));
				newMax = new Location(door.getWorld(), xMax, yMax,          zMax);
			} 
			else
			{
				newMin = new Location(door.getWorld(), xMax, yMin,          zMin);
				newMax = new Location(door.getWorld(), xMax, yMax, (zMax + xLen));
			}
			break;
		}
		door.setMaximum(newMax);
		door.setMinimum(newMin);

		int isOpen = door.getStatus() == true ? 0 : 1; // If door.getStatus() is true (1), set isOpen to 0, as it's just been toggled.
		plugin.getCommander().updateDoorCoords(door.getDoorUID(), isOpen, newMin.getBlockX(), newMin.getBlockY(), newMin.getBlockZ(), newMax.getBlockX(), newMax.getBlockY(), newMax.getBlockZ());
	}

	// Toggle the open status of a door.
	@Override
	public void toggleOpen(Door door)
	{
		door.setStatus(!door.getStatus());
	}
}
