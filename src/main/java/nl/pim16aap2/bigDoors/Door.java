package nl.pim16aap2.bigDoors;

import java.io.Serializable;

import org.bukkit.Location;
import org.bukkit.World;

public class Door implements Serializable
{

	private static final long serialVersionUID = 1L;
	private int xMin, yMin, zMin, xMax, yMax, zMax;
	private int engineX, engineY, engineZ;
	private String name;
	private boolean isOpen, isAvailable;
	private World world;

	// Generate a new door.
	public Door(World world, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int engineX, int engineY,
			int engineZ, String name, boolean isOpen)
	{
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
		this.isAvailable = true;
	}

	// Return the name of this door.
	public String getName()
	{
		return name;
	}

	// Change the availability-status of this door.
	public void changeAvailability(boolean availability)
	{
		this.isAvailable = availability;
	}

	// Return the availability-status of this door.
	public boolean isAvailable()
	{
		return isAvailable;
	}

	// Change the open-status of this door.
	public void changeStatus(boolean status)
	{
		this.isOpen = status;
	}

	// Return the open-status of this door.
	public boolean getStatus()
	{
		return isOpen;
	}

	// Change the open-status of this door.
	public void setStatus(boolean bool)
	{
		isOpen = bool;
	}

	// Return the world this door is located in.
	public World getWorld()
	{
		return world;
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
		// String door = name+"\n isOpen: "+isOpen+"\n World:
		// "+world.getName().toString()+"\n Coords:\n Minimum: "+xMin+" "+yMin+"
		// "+zMin+"\n Maximum: "+xMax+" "+yMax+" "+zMax+"\n Engine: "+engineX+"
		// "+engineY+" "+engineZ;
		String door = name + " " + isOpen + " " + world.getName().toString() + " " + xMin + " " + yMin + " " + zMin
				+ " " + xMax + " " + yMax + " " + zMax + " " + engineX + " " + engineY + " " + engineZ;
		return door;
	}
}
