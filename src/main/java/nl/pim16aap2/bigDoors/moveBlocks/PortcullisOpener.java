package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class PortcullisOpener implements Opener
{
	private BigDoors plugin;
	
	DoorDirection ddirection;

	public PortcullisOpener(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	// Get the direction the door is currently facing as seen from the engine to the end of the door.
	@Override
	public DoorDirection getCurrentDirection(Door door)
	{
		return null;
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

		// Make sure the doorSize does not exceed the total doorSize.
		// If it does, open the door instantly.
		int maxDoorSize = plugin.getConfigLoader().getInt("maxDoorSize");
		if (maxDoorSize != -1)
			if(getDoorSize(door) > maxDoorSize)
				instantOpen = true;

		int blocksToMove = getBlocksToMove(door);
		
		if (blocksToMove != 0)
		{
			// Change door availability so it cannot be opened again (just temporarily, don't worry!).
			plugin.getCommander().setDoorBusy(door.getDoorUID());
			
			new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove);

			// Tell the door object it has been opened and what its new coordinates are.
			toggleOpen  (door);
			updateCoords(door, null, blocksToMove > 0 ? RotateDirection.UP : RotateDirection.DOWN, blocksToMove);
		}
		return true;
	}
	
	public int getBlocksInDir(Door door, RotateDirection upDown)
	{
		int xMin, xMax, zMin, zMax, yMin, yMax, yLen, blocksUp = 0, delta;
		xMin = door.getMinimum().getBlockX();
		yMin = door.getMinimum().getBlockY();
		zMin = door.getMinimum().getBlockZ();
		xMax = door.getMaximum().getBlockX();
		yMax = door.getMaximum().getBlockY();
		zMax = door.getMaximum().getBlockZ();
		yLen = yMax - yMin + 1;
		
		int xAxis, yAxis, zAxis, yGoal;
		World world = door.getWorld();
		delta = upDown == RotateDirection.DOWN ? -1 : 1;
		yAxis = upDown == RotateDirection.DOWN ? yMin - 1 : yMax + 1;
		yGoal = upDown == RotateDirection.DOWN ? yMin - yLen - 1 : yMax + yLen + 1;
			
		while (yAxis != yGoal)
		{
			for (xAxis = xMin; xAxis <= xMax; ++xAxis)
				for (zAxis = zMin; zAxis <= zMax; ++zAxis)
					if (!Util.isAir(world.getBlockAt(xAxis, yAxis, zAxis).getType()))
						return blocksUp;
			yAxis    += delta;
			blocksUp += delta;
		}
		return blocksUp;
	}
	
	public int getBlocksToMove(Door door)
	{
		int blocksUp    = getBlocksInDir(door, RotateDirection.UP  );
		int blocksDown  = getBlocksInDir(door, RotateDirection.DOWN);
		return blocksUp > -1 * blocksDown ? blocksUp : blocksDown;
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
				
		Location newMax = new Location(door.getWorld(), xMax, yMax + moved, zMax);
		Location newMin = new Location(door.getWorld(), xMin, yMin + moved, zMin);
		
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
