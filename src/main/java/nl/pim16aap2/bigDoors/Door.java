package nl.pim16aap2.bigDoors;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.util.DoorDirection;

public class Door
{
	private String                   name;
	private int                      type;
	private World                   world;
	private boolean                isOpen;
	private UUID                   player;
	private long                  doorUID;
	private boolean              isLocked;
	private DoorDirection      engineSide;
	private int                permission;
	private int          xMin, yMin, zMin;
	private int          xMax, yMax, zMax;
	private int engineX, engineY, engineZ;
	private int  powerBlockX, powerBlockY, 
	                          powerBlockZ;

	// TODO: Store permission level.
	// Player is then stored as the person who requested this door, rather than as the owner of the door.
	
	// Generate a new door.
	public Door(UUID player, World world, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int engineX, int engineY,
			int engineZ, String name, boolean isOpen, long doorUID, boolean isLocked, int permission, int type, DoorDirection engineSide,
			int powerBlockX, int powerBlockY, int powerBlockZ)
	{
		this.player      = player;
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
		this.permission  = permission;
		this.type        = type;
		this.engineSide  = engineSide;
		this.powerBlockX = powerBlockX;
		this.powerBlockY = powerBlockY;
		this.powerBlockZ = powerBlockZ;
	}
	
	public Door(UUID player, World world, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int engineX, int engineY,
			int engineZ, String name, boolean isOpen, long doorUID, boolean isLocked, int permission, int type, 
			int powerBlockX, int powerBlockY, int powerBlockZ)
	{
		this(player, world, xMin, yMin, zMin, xMax, yMax, zMax, 
				engineX, engineY, engineZ, name, isOpen, doorUID, isLocked, permission, type, null, powerBlockX, powerBlockY, powerBlockZ);
	}
	
	// Create a door with a player UUID string instead of player Object.
	public Door(World world, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int engineX, int engineY,
			int engineZ, String name, boolean isOpen, long doorUID, boolean isLocked, int permission, String player, int type,
			int powerBlockX, int powerBlockY, int powerBlockZ)
	{
		this(UUID.fromString(player), world, xMin, yMin, zMin, xMax, yMax, zMax, 
				engineX, engineY, engineZ, name, isOpen, doorUID, isLocked, permission, type, null, powerBlockX, powerBlockY, powerBlockZ);
	}
	
	// Create a door with a player UUID string instead of player Object and an engineSide (for draw bridges).
	public Door(World world, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int engineX, int engineY,
			int engineZ, String name, boolean isOpen, long doorUID, boolean isLocked, int permission, String player, int type, 
			DoorDirection engineSide, int powerBlockX, int powerBlockY, int powerBlockZ)
	{
		this(UUID.fromString(player), world, xMin, yMin, zMin, xMax, yMax, zMax, 
				engineX, engineY, engineZ, name, isOpen, doorUID, isLocked, permission, type, engineSide, powerBlockX, powerBlockY, powerBlockZ);
	}
	
//	public Door(String str)
//	{
//		String[] strs = str.trim().split("\\s+");
//
//		int mod = 0;
//		
//		if (strs.length == 12) // Old version.
//		{
//			player = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935"); // Make pim16aap2 the owner (as backup).
//			mod    = 1;
//		}
//		else 
//			player  = UUID.fromString(strs[0]);
//		if (player == null)
//			player = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935");
//		name    = strs[1 - mod];
//		isOpen  = Boolean.getBoolean(strs[2 - mod]);
//		world   = Bukkit.getServer().getWorld(strs[3 - mod]);
//		xMin    = Integer.parseInt(strs[4   - mod]);
//		yMin    = Integer.parseInt(strs[5   - mod]);
//		zMin    = Integer.parseInt(strs[6   - mod]);
//		xMax    = Integer.parseInt(strs[7   - mod]);
//		yMax    = Integer.parseInt(strs[8   - mod]);
//		zMax    = Integer.parseInt(strs[9   - mod]);
//		engineX = Integer.parseInt(strs[10  - mod]);
//		engineY = Integer.parseInt(strs[11  - mod]);
//		engineZ = Integer.parseInt(strs[12  - mod]);
//	}
	
	// ----------------------- SIMPLE GETTERS -----------------------	//
	public int getType()      			{ 	return type;			}	// Get this door's type.
	public String getName() 				{ 	return name; 		}	// Get the name of the door.
	public World getWorld()				{	return world;		}	// Get the world this door is in.
	public long getDoorUID() 			{ 	return doorUID; 		}	// Get doorUID as used in the doors table in the db.
	public boolean isLocked() 			{	return isLocked;		}	// Check if this door is locked or not.
	public boolean getStatus()			{	return isOpen;		}	// Check if this door is in the open or closed state.
	public int getPermission()			{	return permission;	}	// Check permission level of current owner.
	public UUID getPlayerUUID()			{	return player; 		}	// Get UUID of the owner of the door. Might be null!
	public DoorDirection getEngSide()	{	return engineSide;	}	// Get this door's (or drawbridge's, in this case) engine side.
	
	public void setPlayerUUID(UUID playerUUID)	
	{	
		this.player = playerUUID;
	}

	// Change the open-status of this door.
	public void setStatus(boolean bool)
	{
		isOpen = bool;
	}
	
	// Return the location of the powerblock of this door.
	public Location getPowerBlockLoc()
	{
		return new Location(world, powerBlockX, powerBlockY, powerBlockZ);
	}

	// Return the location of the engine of this door.
	public Location getEngine()
	{
		return new Location(world, engineX, engineY, engineZ);
	}

	// Return the minimum values of this door.
	public Location getMinimum()
	{
		return new Location(world, xMin, yMin, zMin);
	}

	// Return the maximum values of this door.
	public Location getMaximum()
	{
		return new Location(world, xMax, yMax, zMax);
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
					+ engineZ + " "
					+ type;
		return door;
	}
	
	public void setLock(boolean lock)
	{
		this.isLocked = lock;
	}
	
	
	public void setPermission(int permission)
	{
		this.permission = permission;
	}

	public void setEngineSide(DoorDirection newEngSide)
	{
		this.engineSide = newEngSide;
	}

	public int getLength()
	{
		int xLen = Math.abs(xMax - xMin);
		int yLen = Math.abs(yMax - yMin);
		int zLen = Math.abs(zMax - zMin);
		if (this.engineSide.equals(DoorDirection.NORTH) || this.engineSide.equals(DoorDirection.SOUTH))
			return zLen > yLen ? zLen : yLen;
		return xLen > yLen ? xLen : yLen;
	}
}
