package nl.pim16aap2.bigDoors;

import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class Door
{
	private Location              min;
	private Location              max;
	private String               name;
	private DoorType             type;
	private World               world;
	private boolean             canGo;
	private boolean            isOpen;
	private Location           engine;
	private UUID               player;
	private long              doorUID;
	private RotateDirection   openDir;
	private boolean          isLocked;
	private DoorDirection  engineSide;
	private Location       powerBlock;
	private Integer     roundedLength;
	private int            permission;
	private Location         chunkLoc;
	private Integer            length;

	// Generate a new door.
	public Door(UUID player, World world, Location min, Location max, Location engine, String name, boolean isOpen, long doorUID, 
			boolean isLocked, int permission, DoorType type, DoorDirection engineSide, Location powerBlock, RotateDirection openDir)
	{
		this.player        = player;
		this.world         = world;
		this.min           = min;
		this.max           = max;
		this.engine        = engine;
		this.powerBlock    = powerBlock;
		this.name          = name;
		this.isOpen        = isOpen;
		this.doorUID       = doorUID;
		this.isLocked      = isLocked;
		this.permission    = permission;
		this.type          = type;
		this.engineSide    = engineSide;
		this.chunkLoc      = null;
		this.length        = null;
		this.roundedLength = null;
		this.canGo         = true;
		this.openDir       = openDir == null ? RotateDirection.NONE : openDir;
	}
	
	public Door(UUID player, World world, Location min, Location max, Location engine, String name, boolean isOpen, long doorUID, 
			boolean isLocked, int permission, DoorType type, 	Location powerBlock, RotateDirection openDir)
	{
		this(player, world, min, max, engine, name, isOpen, doorUID, isLocked, permission, type, null, powerBlock, openDir);
	}
	
	// Create a door with a player UUID string instead of player Object.
	public Door(World world, Location min, Location max, Location engine, String name, boolean isOpen, long doorUID, 
			boolean isLocked, int permission, String player, DoorType type, Location powerBlock, RotateDirection openDir)
	{
		this(UUID.fromString(player), world, min, max, engine, name, isOpen, 
				doorUID, isLocked, permission, type, null, powerBlock, openDir);
	}
	
	// Create a door with a player UUID string instead of player Object and an engineSide (for draw bridges).
	public Door(World world, Location min, Location max, Location engine, String name, boolean isOpen, long doorUID,
			boolean isLocked, int permission, String player, DoorType type, DoorDirection engineSide, Location powerBlock, 
			RotateDirection openDir)
	{
		this(UUID.fromString(player), world, min, max, engine, name, isOpen, doorUID, isLocked, permission, 
				type, engineSide, powerBlock, openDir);
	}
	
	// ----------------------- SIMPLE GETTERS -----------------------	//
	public DoorType getType()   			{ 	return type;			}	// Get this door's type.
	public String getName() 				{ 	return name; 		}	// Get the name of the door.
	public World getWorld()				{	return world;		}	// Get the world this door is in.
	public long getDoorUID() 			{ 	return doorUID; 		}	// Get doorUID as used in the doors table in the db.
	public boolean isLocked() 			{	return isLocked;		}	// Check if this door is locked or not.
	public boolean isOpen()				{	return isOpen;		}	// Check if this door is in the open or closed state.
	public int getPermission()			{	return permission;	}	// Check permission level of current owner.
	public UUID getPlayerUUID()			{	return player; 		}	// Get UUID of the owner of the door. Might be null!
	public DoorDirection getEngSide()	{	return engineSide;	}	// Get this door's (or drawbridge's, in this case) engine side.
	public boolean canGo()				{	return canGo;		}	// Check if this door can still be opened or not.
	public RotateDirection getOpenDir()	{	return openDir;		}	// Get the open direction of this door.
	
	public void setPlayerUUID(UUID playerUUID)	
	{	
		this.player = playerUUID;
	}

	// Change the open-status of this door.
	public void setOpenStatus(boolean bool)
	{
		isOpen = bool;
	}
	
	public void setCanGo(boolean bool)
	{
		this.canGo = bool;
	}
	
	// Return the location of the powerblock of this door.
	public Location getPowerBlockLoc()
	{
		return powerBlock;
	}

	// Return the location of the engine of this door.
	public Location getEngine()
	{
		return engine;
	}

	// Return the minimum values of this door.
	public Location getMinimum()
	{
		return min;
	}

	// Return the maximum values of this door.
	public Location getMaximum()
	{
		return max;
	}

	// Change the minimum values of this door.
	public void setMinimum(Location loc)
	{
		this.min.setX(loc.getX());
		this.min.setY(loc.getY());
		this.min.setZ(loc.getZ());
	}

	// Change the maximum values of this door.
	public void setMaximum(Location loc)
	{
		this.max.setX(loc.getX());
		this.max.setY(loc.getY());
		this.max.setZ(loc.getZ());
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
	
	public Location getChunkCoords()
	{
		if (this.chunkLoc != null)
			return this.chunkLoc;
		
		Chunk chunk   = this.world.getBlockAt(new Location(this.world, engine.getX(), engine.getY(), engine.getZ())).getChunk();
		this.chunkLoc = new Location(this.world, chunk.getX(), 0, chunk.getZ());
		
		return chunkLoc;
	}
	
	public boolean chunkInRange(Chunk chunk)
	{
		getChunkCoords();
		getLength();

		int difX = Math.abs(chunk.getX() - this.chunkLoc.getBlockX());
		int difZ = Math.abs(chunk.getZ() - this.chunkLoc.getBlockZ());
		
		return difX <= this.roundedLength && difZ <= this.roundedLength;
	}

	public int getLength()
	{
		if (this.length != null)
			return this.length;
		int xLen = Math.abs(this.max.getBlockX() - this.min.getBlockX());
		int zLen = Math.abs(this.max.getBlockZ() - this.min.getBlockZ());
		
		this.length = 1;
		// Regular door or Portcullis
		if (this.type.equals(DoorType.DOOR) || this.type.equals(DoorType.PORTCULLIS))
			this.length = xLen > zLen ? xLen : zLen;

		// Drawbridge
		else if (this.type.equals(DoorType.DRAWBRIDGE) && this.engineSide != null)
		{
			int yLen = Math.abs(this.max.getBlockY() - this.min.getBlockY());
			if (this.engineSide.equals(DoorDirection.NORTH) || this.engineSide.equals(DoorDirection.SOUTH))
				this.length = zLen > yLen ? zLen : yLen;
			else
				this.length = xLen > yLen ? xLen : yLen;
		}
		
		// Portcullis engine is in the middle and doesn't rotate.
		if (this.type.equals(DoorType.PORTCULLIS))
			this.length /= 2;
		
		this.roundedLength = this.length / 16 + 1; 
		
		return this.length;
	}
	
	public DoorDirection getLookingDir()
	{
		if (this.type.equals(DoorType.DRAWBRIDGE) || this.type.equals(DoorType.PORTCULLIS))
			return engineSide == DoorDirection.NORTH || engineSide == DoorDirection.SOUTH ? 
					DoorDirection.NORTH : DoorDirection.EAST;
		
		return 	engine.getBlockZ() != min.getBlockZ() ? DoorDirection.NORTH :
			    engine.getBlockX() != max.getBlockX() ? DoorDirection.EAST  :
			    engine.getBlockZ() != max.getBlockZ() ? DoorDirection.SOUTH :
			    engine.getBlockX() != min.getBlockX() ? DoorDirection.WEST  : null;
	}
}
