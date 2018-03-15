package nl.pim16aap2.bigDoors;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.util.Util;

public class Commander
{
	@SuppressWarnings("unused")
	private final BigDoors plugin;
	private ArrayList<Integer> busyDoors;
	private boolean goOn   = true;
	private boolean paused = false;
	private SQLiteJDBCDriverConnection db;
	
	public Commander(BigDoors plugin, SQLiteJDBCDriverConnection db)
	{
		this.plugin = plugin;
		this.db     = db;
		busyDoors   = new ArrayList<Integer>();
	}
	
	// Get the database;
	private SQLiteJDBCDriverConnection getRDatabase()
	{
		return db;
	}

	// Check if a door is busy
	public boolean isDoorBusy(int doorUID)
	{
		for (int x : busyDoors)
		{
			if (x == doorUID)
				return true;
		}
		return false;
	}
	
	// Change the busy-status of a door.
	public void setDoorBusy(int doorUID)
	{
		busyDoors.add(doorUID);
	}
	
	// Set the availability of the door.
	public void setDoorAvailable(int doorUID)
	{
		busyDoors.remove((Integer) doorUID);
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
			int doorUID = Integer.parseInt(doorStr);
			return getRDatabase().getDoor(doorUID);
		}
		// If it can't convert to an int, get all doors from the player with the provided name. 
		// If there is more than one, tell the player that they are going to have to make a choice.
		catch (NumberFormatException e)
		{
			ArrayList<Door> doors = new ArrayList<Door>();
			doors = getRDatabase().getDoors(player.getUniqueId().toString(), doorStr);
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

	// Add a door to the list of doors.
	public void addDoor(Door newDoor, Player player)
	{
		getRDatabase().insert(newDoor, player.getUniqueId().toString());
	}
	
	public void removeDoor(int doorUID)
	{
		getRDatabase().removeDoor(doorUID);
	}

	// Check if a given name is already in use or not.
	public boolean isNameAvailable(String name, Player player)
	{
		return getRDatabase().isNameAvailable(name, player.getUniqueId().toString());
	}

	// Remove all doors named doorName owned by player playerUUID. doorName can not be null!
	public void removeDoors(String playerUUID, String doorName)
	{
		getRDatabase().removeDoors(playerUUID, doorName);
	}

	// Returns the number of doors owner by a player and with a specific name, if provided (can be null).
	public long countDoors(String playerUUID, String doorName)
	{
		return getRDatabase().countDoors(playerUUID, doorName);
	}

	// Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
	public ArrayList<Door> getDoors(String playerUUID, String name)
	{
		return getRDatabase().getDoors(playerUUID, name);
	}

	// Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
	public ArrayList<Door> getDoorsInRange(String playerUUID, String name, int start, int end)
	{
		return getRDatabase().getDoors(playerUUID, name, start, end);
	}

	// Get a door with a specific doorUID.
	public Door getDoor(int doorUID)
	{
		return getRDatabase().getDoor(doorUID);
	}

	// Update the coordinates of a given door.
	public void updateDoorCoords(int doorUID, int isOpen, int blockXMin, int blockYMin, int blockZMin, int blockXMax, int blockYMax, int blockZMax)
	{
		getRDatabase().updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax, blockYMax, blockZMax);
	}

	// Get a door from the x,y,z coordinates of its engine block (= rotation point at lowest y).
	public Door doorFromEngineLoc(int engineX, int engineY, int engineZ)
	{
		return getRDatabase().doorFromEngineLoc(engineX, engineY, engineZ);
	}
}
