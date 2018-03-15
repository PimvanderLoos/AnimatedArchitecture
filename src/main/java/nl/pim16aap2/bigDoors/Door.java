package nl.pim16aap2.bigDoors;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Door implements Serializable
{

	private static final long serialVersionUID = 1L;
	private int xMin, yMin, zMin, xMax, yMax, zMax;
	private int engineX, engineY, engineZ;
	private String name;
	private boolean isOpen;
	private World world;
	private UUID player;
	private int doorUID;
	private boolean isLocked;

	// TODO: Store permission level.
	// Player is then stored as the person who requested this door, rather than as the owner of the door.
	
	// Generate a new door.
	public Door(Player player, World world, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int engineX, int engineY,
			int engineZ, String name, boolean isOpen, int doorUID, boolean isLocked)
	{
		this.player      = player == null ? null : player.getUniqueId();
		this.world       = world;
		this.xMin        = xMin;
		this.yMin        = yMin;
		this.zMin        = zMin;
		this.xMax        = xMax;
		this.yMax        = yMax;
		this.zMax        = zMax;
		this.engineX     = engineX;
		this.engineY     = engineY;
		this.engineZ     = engineZ;
		this.name        = name;
		this.isOpen      = isOpen;
		this.doorUID     = doorUID;
		this.isLocked    = isLocked;
	}

	// Generate a new door without a provided doorUID.
	public Door(Player player, World world, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int engineX, int engineY,
			int engineZ, String name, boolean isOpen)
	{
		this(player,world,xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, name, isOpen, -1, false);
	}
	
	public Door(String str)
	{
		String[] strs = str.trim().split("\\s+");

		int mod = 0;
		
		if (strs.length == 12) // Old version.
		{
			player = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935"); // Make pim16aap2 the owner.
			mod    = 1;
		}
		else 
			player  = UUID.fromString(strs[0]);
		if (player == null)
			player = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935");
		name    = strs[1 - mod];
		isOpen  = Boolean.getBoolean(strs[2 - mod]);
		world   = Bukkit.getServer().getWorld(strs[3 - mod]);
		xMin    = Integer.parseInt(strs[4   - mod]);
		yMin    = Integer.parseInt(strs[5   - mod]);
		zMin    = Integer.parseInt(strs[6   - mod]);
		xMax    = Integer.parseInt(strs[7   - mod]);
		yMax    = Integer.parseInt(strs[8   - mod]);
		zMax    = Integer.parseInt(strs[9   - mod]);
		engineX = Integer.parseInt(strs[10  - mod]);
		engineY = Integer.parseInt(strs[11  - mod]);
		engineZ = Integer.parseInt(strs[12  - mod]);
	}
	
	// ------------------- SIMPLE GETTERS ------------------ //
	public UUID getPlayer() 		{	return player; 		}	// Get UUID of the owner of the door. Might be null!
	public int getDoorUID() 		{ 	return doorUID; 		}	// Get doorUID as used in the doors table in the db.
	public String getName() 		{ 	return name; 		}	// Get the name of the door.
	public boolean getStatus()	{	return isOpen;		}	// Check if this door is in the open or closed state.
	public World getWorld()		{	return world;		}	// Get the world this door is in.

	// Change the open-status of this door.
	public void setStatus(boolean bool)
	{
		isOpen = bool;
	}

	// Return the location of the engine of this door.
	public Location getEngine()
	{
		Location engineLoc = new Location(world, engineX, engineY, engineZ);
		return engineLoc;
	}

	// Return the minimum values of this door.
	public Location getMinimum()
	{
		Location minima = new Location(world, xMin, yMin, zMin);
		return minima;
	}

	// Return the maximum values of this door.
	public Location getMaximum()
	{
		Location maxima = new Location(world, xMax, yMax, zMax);
		return maxima;
	}

	// Change the minimum values of this door.
	public void setMinimum(Location loc)
	{
		this.xMin = loc.getBlockX();
		this.yMin = loc.getBlockY();
		this.zMin = loc.getBlockZ();
	}

	// Change the maximum values of this door.
	public void setMaximum(Location loc)
	{
		this.xMax = loc.getBlockX();
		this.yMax = loc.getBlockY();
		this.zMax = loc.getBlockZ();
	}

	// Return this object as a string.
	@Override
	public String toString()
	{
		String door = name + " " 
					+ isOpen + " " 
					+ (world == null ? "null" : world.getUID().toString()) + " " 
					+ xMin + " " 
					+ yMin + " " 
					+ zMin + " " 
					+ xMax + " " 
					+ yMax + " " 
					+ zMax + " " 
					+ engineX + " " 
					+ engineY + " " 
					+ engineZ;
		return door;
	}

	public boolean isLocked()
	{
		return this.isLocked;
	}
	
	public void setLock(boolean lock)
	{
		this.isLocked = lock;
	}
}
