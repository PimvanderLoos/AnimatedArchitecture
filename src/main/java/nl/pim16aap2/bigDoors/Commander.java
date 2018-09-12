package nl.pim16aap2.bigDoors;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class Commander
{
	@SuppressWarnings("unused")
	private final BigDoors plugin;
	private ArrayList<Long> busyDoors;
	private boolean goOn   = true;
	private boolean paused = false;
	private SQLiteJDBCDriverConnection db;
	
	public Commander(BigDoors plugin, SQLiteJDBCDriverConnection db)
	{
		this.plugin = plugin;
		this.db     = db;
		busyDoors   = new ArrayList<Long>();
	}

	// Check if a door is busy
	public boolean isDoorBusy(long doorUID)
	{
		for (long x : busyDoors)
		{
			if (x == doorUID)
				return true;
		}
		return false;
	}
	
	public void emptyBusyDoors()
	{
		busyDoors.clear();
	}
	
	// Change the busy-status of a door.
	public void setDoorBusy(long doorUID)
	{
		busyDoors.add(doorUID);
	}
	
	// Set the availability of the door.
	public void setDoorAvailable(long doorUID)
	{
		busyDoors.remove((Long) doorUID);
	}

	// Check if the doors are paused.
	public boolean isPaused()
	{
		return this.paused;
	}
	
	// Toggle the paused status of all doors.
	public void togglePaused()
	{
		this.paused = !this.paused;
	}
	
	// Check if the doors can go. This differs from beign paused in that it will finish up all currently moving doors.
	public boolean canGo()
	{
		return this.goOn;
	}
	
	// Change the canGo status of all doors.
	public void setCanGo(boolean bool)
	{
		this.goOn = bool;
	}

	// Print an ArrayList of doors to a player.
	public void printDoors(Player player, ArrayList<Door> doors)
	{
		for (Door door : doors)
			Util.messagePlayer(player, door.getDoorUID() + ": " + door.getName().toString());
	}

	// Get the door from the string. Can be use with a doorUID or a doorName.
	public Door getDoor(String doorStr, Player player)
	{
		// First try converting the doorStr to a doorUID.
		try
		{
			long doorUID = Long.parseLong(doorStr);
			return db.getDoor(doorUID);
		}
		// If it can't convert to a long, get all doors from the player with the provided name. 
		// If there is more than one, tell the player that they are going to have to make a choice.
		catch (NumberFormatException e)
		{
			if (player == null)
				return null;
			ArrayList<Door> doors = new ArrayList<Door>();
			doors = db.getDoors(player.getUniqueId().toString(), doorStr);
			if (doors.size() == 1)
				return doors.get(0);
			else 
			{
				Util.messagePlayer(player, "More than 1 door with that name found! Please use its ID instead!");
				printDoors(player, doors);
				return null;
			}
		}
	}

	public void addDoor(Door newDoor)
	{
		db.insert(newDoor);
	}

	public void addDoor(Door newDoor, Player player, int permission)
	{
		if (newDoor.getPlayerUUID() != player.getUniqueId())
			newDoor.setPlayerUUID(player.getUniqueId());
		if (newDoor.getPermission() != permission)
			newDoor.setPermission(permission);
		db.insert(newDoor);
	}
	
	// Add a door to the db of doors.
	public void addDoor(Door newDoor, Player player)
	{
		addDoor(newDoor, player, 0);
	}
	
	public void removeDoor(long doorUID)
	{
		db.removeDoor(doorUID);
	}
	
	public void removeDoor(String playerUUID, String doorName)
	{
		db.removeDoor(playerUUID, doorName);
	}

	// Returns the number of doors owner by a player and with a specific name, if provided (can be null).
	public long countDoors(String playerUUID, String doorName)
	{
		return db.countDoors(playerUUID, doorName);
	}
	
	// Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
	public ArrayList<Door> getDoors(String playerUUID, String name)
	{
		return playerUUID == null ? getDoors(name) : db.getDoors(playerUUID, name);
	}

	// Returns an ArrayList of doors with a specific name.
	private ArrayList<Door> getDoors(String name)
	{
		return db.getDoors(name);
	}

	// Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
	public ArrayList<Door> getDoorsInRange(String playerUUID, String name, int start, int end)
	{
		return db.getDoors(playerUUID, name, start, end);
	}

	// Get a door with a specific doorUID.
	public Door getDoor(long doorUID)
	{
		return db.getDoor(doorUID);
	}
	
	// Get the permission of a player on a door.
	public int getPermission(String playerUUID, long doorUID)
	{
		return db.getPermission(playerUUID, doorUID);
	}

	// Update the coordinates of a given door.
	public void updateDoorCoords(long doorUID, int isOpen, int blockXMin, int blockYMin, int blockZMin, int blockXMax, int blockYMax, int blockZMax)
	{
		db.updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax, blockYMax, blockZMax);
	}

	// Update the coordinates of a given door.
	public void updateDoorCoords(long doorUID, int isOpen, int blockXMin, int blockYMin, int blockZMin, int blockXMax, int blockYMax, int blockZMax, DoorDirection newEngSide)
	{
		db.updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax, blockYMax, blockZMax, newEngSide);
	}
	
	// Change the "locked" status of a door.
	public void setLock(long doorUID, boolean newLockStatus)
	{
		db.setLock(doorUID, newLockStatus);
	}
	
//	// Get a door from the x,y,z coordinates of its engine block (= rotation point at lowest y).
//	public Door doorFromEngineLoc(Location loc)
//	{
//		return db.doorFromEngineLoc(loc);
//	}

	// Get a door from the x,y,z coordinates of its power block.
	public Door doorFromPowerBlockLoc(Location loc)
	{
		return db.doorFromPowerBlockLoc(loc);
	}

	// Change hte location of a powerblock.
	public void updatePowerBlockLoc(long doorUID, Location loc)
	{
		db.updateDoorPowerBlockLoc(doorUID, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public boolean isPowerBlockLocationValid(Location loc)
	{
		return db.isPowerBlockLocationEmpty(loc);
	}
}
