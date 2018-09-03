package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class BridgeOpener implements Opener
{
	private BigDoors        plugin;
	private RotateDirection upDown;

	public BridgeOpener(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	// Check if the new position is free.
	public boolean isNewPosFree(Door door, RotateDirection upDown, DoorDirection cardinal)
	{
		int startX = 0, startY = 0, startZ = 0;
		int stopX  = 0, stopY  = 0, stopZ  = 0;
		World world = door.getWorld();

		if (upDown.equals(RotateDirection.UP))
		{
			switch (cardinal)
			{
			// North West = Min X, Min Z
			// South West = Min X, Max Z
			// North East = Max X, Min Z
			// South East = Max X, Max X
			case NORTH:
//				Bukkit.broadcastMessage("U: NORTH");
				startX = door.getMinimum().getBlockX();
				stopX  = door.getMaximum().getBlockX();
				
				startY = door.getMinimum().getBlockY() + 1;
				stopY  = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();
				
				startZ = door.getMinimum().getBlockZ();
				stopZ  = door.getMinimum().getBlockZ();
				break;
				
			case SOUTH:
//				Bukkit.broadcastMessage("U: SOUTH");
				startX = door.getMinimum().getBlockX();
				stopX  = door.getMaximum().getBlockX();
				
				startY = door.getMinimum().getBlockY() + 1;
				stopY  = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();
				
				startZ = door.getMaximum().getBlockZ();
				stopZ  = door.getMaximum().getBlockZ();
				break;
				
			case EAST:
//				Bukkit.broadcastMessage("U: EAST");
				startX = door.getMaximum().getBlockX();
				stopX  = door.getMaximum().getBlockX();
				
				startY = door.getMinimum().getBlockY() + 1;
				stopY  = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
				
				startZ = door.getMinimum().getBlockZ();
				stopZ  = door.getMaximum().getBlockZ();
				break;
				
			case WEST:
//				Bukkit.broadcastMessage("U: WEST");
				startX = door.getMinimum().getBlockX();
				stopX  = door.getMinimum().getBlockX();
				
				startY = door.getMinimum().getBlockY() + 1;
				stopY  = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
				
				startZ = door.getMinimum().getBlockZ();
				stopZ  = door.getMaximum().getBlockZ();
				break;
			}
		}
		else
		{
			switch (cardinal)
			{
			// North West = Min X, Min Z
			// South West = Min X, Max Z
			// North East = Max X, Min Z
			// South East = Max X, Max X
			case NORTH:
//				Bukkit.broadcastMessage("D: NORTH");
				startX = door.getMinimum().getBlockX();
				stopX  = door.getMaximum().getBlockX();
				
				startY = door.getMinimum().getBlockY();
				stopY  = door.getMinimum().getBlockY();
				
				startZ = door.getMinimum().getBlockZ() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
				stopZ  = door.getMinimum().getBlockZ() - 1;
				break;
				
			case SOUTH:
//				Bukkit.broadcastMessage("D: SOUTH");
				startX = door.getMinimum().getBlockX();
				stopX  = door.getMaximum().getBlockX();
				
				startY = door.getMinimum().getBlockY();
				stopY  = door.getMinimum().getBlockY();
				
				startZ = door.getMinimum().getBlockZ() + 1;
				stopZ  = door.getMinimum().getBlockZ() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();
				break;
				
			case EAST:
//				Bukkit.broadcastMessage("D: EAST");
				startX = door.getMinimum().getBlockX() + 1;
				stopX  = door.getMaximum().getBlockX() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();
				
				startY = door.getMinimum().getBlockY();
				stopY  = door.getMinimum().getBlockY();
				
				startZ = door.getMinimum().getBlockZ();
				stopZ  = door.getMaximum().getBlockZ();
				break;
				
			case WEST:
//				Bukkit.broadcastMessage("D: WEST");
				startX = door.getMinimum().getBlockX() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
				stopX  = door.getMinimum().getBlockX() - 1;
				
				startY = door.getMinimum().getBlockY();
				stopY  = door.getMinimum().getBlockY();
				
				startZ = door.getMinimum().getBlockZ();
				stopZ  = door.getMaximum().getBlockZ();
				break;
			}
		}
		
		int x = startX, y, z;
		while (x <= stopX)
		{
			y = startY;
			while (y <= stopY)
			{
				z = startZ;
				while (z <= stopZ)
				{			
					if (world.getBlockAt(x, y, z).getType() != Material.AIR)
					{
//						Bukkit.broadcastMessage(ChatColor.RED + "Found a non-air block of the type " + world.getBlockAt(x, y, z).getType().toString() + ". Stopping checks!");
						return false;
					}
					++z;
				}
				++y;
			}
			++x;
		}
		return true;
	}

	// Check if the bridge should go up or down.
	public RotateDirection getUpDown(Door door)
	{
		int height = Math.abs(door.getMinimum().getBlockY() - door.getMaximum().getBlockY());
		if (height > 0)
			return RotateDirection.DOWN;
		return RotateDirection.UP;
	}
	
	// Figure out which way the bridge should go.
	public DoorDirection getOpenDirection(Door door)
	{
		RotateDirection upDown = getUpDown(door);
//		Bukkit.broadcastMessage("0: UpDown for door \"" + door.getName() + "\" = " + upDown.toString());
		DoorDirection cDir     = getCurrentDirection(door);
		boolean NS  = cDir    == DoorDirection.NORTH || cDir == DoorDirection.SOUTH;
		
//		Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "Current Direction = " + cDir.toString());
		
		if (upDown.equals(RotateDirection.UP))
			return isNewPosFree(door, upDown, door.getEngSide()) ? door.getEngSide() : null;
		
		return 	 NS && isNewPosFree(door, upDown, DoorDirection.NORTH) ? DoorDirection.NORTH :
				!NS && isNewPosFree(door, upDown, DoorDirection.EAST ) ? DoorDirection.EAST  : 
				 NS && isNewPosFree(door, upDown, DoorDirection.SOUTH) ? DoorDirection.SOUTH : 
				!NS && isNewPosFree(door, upDown, DoorDirection.WEST ) ? DoorDirection.WEST  : null;
	}

	// Get the "current direction". In this context this means on which side of the drawbridge the engine is.
	@Override
	public DoorDirection getCurrentDirection(Door door)
	{	
		return door.getEngSide();
	}

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
	public boolean openDoor(Door door, double speed)
	{
		return openDoor(door, speed/1.5, false, false);
	}

	@Override
	public boolean openDoor(Door door, double speed, boolean instantOpen, boolean silent)
	{
		if (plugin.getCommander().isDoorBusy(door.getDoorUID()))
		{
			if (!silent)
			plugin.getMyLogger().myLogger(Level.INFO, "Bridge " + door.getName() + " is not available right now!");
			return true;
		}

		if (!chunksLoaded(door))
		{
			plugin.getMyLogger().logMessage(ChatColor.RED + "Chunk for bridge " + door.getName() + " is not loaded!", true, false);
			return true;
		}

		DoorDirection currentDirection = getCurrentDirection(door);
		if (currentDirection == null)
		{
			plugin.getMyLogger().logMessage("Current direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!", true, false);
			return false;
		}
		this.upDown = getUpDown(door);
//		Bukkit.broadcastMessage("1: UpDown for door \"" + door.getName() + "\" = " + upDown.toString());
		if (upDown == null)
		{
			plugin.getMyLogger().logMessage("UpDown direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!", true, false);
			return false;
		}
		DoorDirection openDirection = getOpenDirection(door);
		if (openDirection == null)
		{
			plugin.getMyLogger().logMessage("OpenDirection direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!", true, false);
			return false;
		}
		
		// Make sure the doorSize does not exceed the total doorSize.
		// If it does, open the door instantly.
		int maxDoorSize = plugin.getConfigLoader().getInt("maxDoorSize");
		if (maxDoorSize != -1)
			if(getDoorSize(door) > maxDoorSize)
				instantOpen = true;
		
		// Change door availability so it cannot be opened again (just temporarily, don't worry!).
		plugin.getCommander().setDoorBusy(door.getDoorUID());

		new BridgeMover(plugin, door.getWorld(), 0.13, door, this.upDown, openDirection, instantOpen);
		
//		// Tell the door object it has been opened and what its new coordinates are.
		toggleOpen  (door);
		updateCoords(door, openDirection, this.upDown, -1);
		return true;
	}

	@Override
	public void updateCoords(Door door, DoorDirection openDirection, RotateDirection upDown, int moved)
	{
		int xMin = door.getMinimum().getBlockX();
		int yMin = door.getMinimum().getBlockY();
		int zMin = door.getMinimum().getBlockZ();
		int xMax = door.getMaximum().getBlockX();
		int yMax = door.getMaximum().getBlockY();
		int zMax = door.getMaximum().getBlockZ();
		int xLen = xMax - xMin;
		int yLen = yMax - yMin;
		int zLen = zMax - zMin;
		Location newMax = null;
		Location newMin = null;
		DoorDirection newEngSide = door.getEngSide();
		
		switch (openDirection)
		{
		case NORTH:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.NORTH;
				newMin = new Location(door.getWorld(), xMin, yMin,        zMin);
				newMax = new Location(door.getWorld(), xMax, yMin + zLen, zMin);
			} 
			else
			{
				newEngSide = DoorDirection.SOUTH;
				newMin = new Location(door.getWorld(), xMin, yMin, zMin - yLen);
				newMax = new Location(door.getWorld(), xMax, yMin, zMin       );
			}
			break;
			
			
		case EAST:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.EAST;
				newMin = new Location(door.getWorld(), xMax, yMin,        zMin);
				newMax = new Location(door.getWorld(), xMax, yMin + xLen, zMax);
			} 
			else
			{
				newEngSide = DoorDirection.WEST;
				newMin = new Location(door.getWorld(), xMax,        yMin, zMin);
				newMax = new Location(door.getWorld(), xMax + yLen, yMin, zMax);
			}
			break;
			
			
		case SOUTH:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.SOUTH;
				newMin = new Location(door.getWorld(), xMin, yMin,        zMax);
				newMax = new Location(door.getWorld(), xMax, yMin + zLen, zMax);
			} 
			else
			{
				newEngSide = DoorDirection.NORTH;
				newMin = new Location(door.getWorld(), xMin, yMin, zMax       );
				newMax = new Location(door.getWorld(), xMax, yMin, zMax + yLen);
			}
			break;
			
			
		case WEST:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.WEST;
				newMin = new Location(door.getWorld(), xMin, yMin,        zMin);
				newMax = new Location(door.getWorld(), xMin, yMin + xLen, zMax);
			} 
			else
			{
				newEngSide = DoorDirection.EAST;
				newMin = new Location(door.getWorld(), xMin - yLen, yMin, zMin);
				newMax = new Location(door.getWorld(), xMin,        yMin, zMax);
			}
			break;
		}
		door.setMaximum(newMax);
		door.setMinimum(newMin);
		door.setEngineSide(newEngSide);

		int isOpen = door.getStatus() == true ? 0 : 1; // If door.getStatus() is true (1), set isOpen to 0, as it's just been toggled.
		plugin.getCommander().updateDoorCoords(door.getDoorUID(), isOpen, newMin.getBlockX(), newMin.getBlockY(), newMin.getBlockZ(), newMax.getBlockX(), newMax.getBlockY(), newMax.getBlockZ(), newEngSide);
	}

	// Toggle the open status of a drawbridge.
	@Override
	public void toggleOpen(Door door)
	{
		door.setStatus(!door.getStatus());
	}
}
