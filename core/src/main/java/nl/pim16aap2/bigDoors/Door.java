package nl.pim16aap2.bigDoors;

import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class Door
{
    private Location              min;
    private Location              max;
    private String               name;
    private DoorType             type;
    private World               world;
    private boolean            isOpen;
    private Location           engine;
    private UUID               player;
    private final long        doorUID;
    private RotateDirection   openDir;
    private boolean          isLocked;
    private int             autoClose;
    private DoorDirection  engineSide;
    private String         playerName;
    private Location       powerBlock;
    private Integer     roundedLength;
    private int            permission;
    private Location         chunkLoc;
    private Integer            length;
    private UUID primeOwner;
    private boolean notify;

    private Integer blockCount = null;
    private int      blocksToMove = 0;

    // Generate a new door.
    public Door(UUID player, String playerName, UUID primeOwner, World world, Location min, Location max, Location engine, String name, boolean isOpen, long doorUID,
                boolean isLocked, int permission, DoorType type, DoorDirection engineSide, Location powerBlock,
                RotateDirection openDir, int autoClose, boolean notify)
    {
        this.player     = player;
        this.world      = world;
        this.min        = min;
        this.max        = max;
        this.engine     = engine;
        this.powerBlock = powerBlock;
        this.playerName = playerName;
        this.name       = name;
        this.isOpen     = isOpen;
        this.doorUID    = doorUID;
        this.isLocked   = isLocked;
        this.permission = permission;
        this.type       = type;
        this.engineSide = engineSide;
        chunkLoc        = null;
        length          = null;
        roundedLength   = null;
        this.openDir    = openDir == null ? RotateDirection.NONE : openDir;
        this.autoClose  = autoClose;
        this.primeOwner = primeOwner;
        this.notify     = notify;
    }

    public Door(UUID player, String playerName, UUID primeOwner, World world, Location min, Location max, Location engine, String name,
                boolean isOpen, long doorUID, boolean isLocked, int permission, DoorType type,
                Location powerBlock, RotateDirection openDir, int autoClose, boolean notify)
    {
        this(player, playerName, primeOwner, world, min, max, engine, name, isOpen, doorUID, isLocked, permission, type, null,
             powerBlock, openDir, autoClose, notify);
    }

    // Create a door with a player UUID string instead of player Object.
    public Door(World world, Location min, Location max, Location engine, String name, boolean isOpen,
                long doorUID, boolean isLocked, int permission, String player, String playerName, UUID primeOwner, DoorType type, Location powerBlock,
                RotateDirection openDir, int autoClose, boolean notify)
    {
        this(UUID.fromString(player), playerName, primeOwner, world, min, max, engine, name, isOpen,
             doorUID, isLocked, permission, type, null, powerBlock, openDir, autoClose, notify);
    }

    // Create a door with a player UUID string instead of player Object and an engineSide (for draw bridges).
    public Door(World world, Location min, Location max, Location engine, String name, boolean isOpen,
                long doorUID, boolean isLocked, int permission, String player, String playerName, UUID primeOwner, DoorType type,
                DoorDirection engineSide, Location powerBlock, RotateDirection openDir, int autoClose, boolean notify)
    {
        this(UUID.fromString(player), playerName, primeOwner, world, min, max, engine, name, isOpen, doorUID, isLocked, permission,
             type, engineSide, powerBlock, openDir, autoClose, notify);
    }

    // ------------------------ SIMPLE GETTERS -------------------- //
    public DoorType getType() throws NullPointerException
    {
        try
        {
            return type;
        }
        catch (NullPointerException e)
        {
            return null;
        }
    }
    public String getName()             {  return name;          }  // Get the name of the door.
    public World getWorld()             {  return world;         }  // Get the world this door is in.
    public long getDoorUID()            {  return doorUID;       }  // Get doorUID as used in the doors table in the db.
    public boolean isLocked()           {  return isLocked;      }  // Check if this door is locked or not.
    public boolean isOpen()             {  return isOpen;        }  // Check if this door is in the open or closed state.
    public int getPermission()          {  return permission;    }  // Check permission level of current owner.
    public UUID getPlayerUUID()         {  return player;        }  // Get UUID of the owner of the door. Might be null!
    public String getPlayerName()       {  return playerName;    }  // Get name of the owner of the door. Might be null!
    public DoorDirection getEngSide()   {  return engineSide;    }  // Get this door's (or drawbridge's, in this case) engine side.
    public RotateDirection getOpenDir() {  return openDir;       }  // Get the open direction of this door.
    public int getAutoClose()           {  return autoClose;     }  // Get the auto close time.
    public int getBlocksToMove()        {  return blocksToMove;  }  // Get the desired number of blocks to move this door.

    public boolean notificationEnabled()
    {
        return notify;
    }

    public void setNotificationEnabled(boolean notify)
    {
        this.notify = notify;
    }

    public UUID getPrimeOwner()
    {
        return this.primeOwner;
    }

    public void setPlayerUUID(UUID playerUUID)
    {
        player = playerUUID;
    }

    public void setOpenDir(RotateDirection newRotDir)
    {
        openDir = newRotDir;
    }

    public void setBlocksToMove(int move)
    {
        blocksToMove = move;
    }

    // Change the open-status of this door.
    public void setOpenStatus(boolean bool)
    {
        isOpen = bool;
    }

    // Return the location of the powerblock of this door.
    public Location getPowerBlockLoc()
    {
        return powerBlock.clone();
    }

    // Return the location of the engine of this door.
    public Location getEngine()
    {
        return engine.clone();
    }

    // Return the minimum values of this door.
    public Location getMinimum()
    {
        return min.clone();
    }

    // Return the maximum values of this door.
    public Location getMaximum()
    {
        return max.clone();
    }

    // Change the minimum values of this door.
    public void setMinimum(Location loc)
    {
        min.setX(loc.getX());
        min.setY(loc.getY());
        min.setZ(loc.getZ());
    }

    // Change the maximum values of this door.
    public void setMaximum(Location loc)
    {
        max.setX(loc.getX());
        max.setY(loc.getY());
        max.setZ(loc.getZ());
    }

    public void setLock(boolean lock)
    {
        isLocked = lock;
    }

    public void setPermission(int permission)
    {
        this.permission = permission;
    }

    public void setEngineSide(DoorDirection newEngSide)
    {
        engineSide = newEngSide;
    }

    public Location getChunkCoords()
    {
        if (chunkLoc != null)
            return chunkLoc;

        chunkLoc = new Location(world, engine.getBlockX() >> 4, 0, engine.getBlockZ() >> 4);
        return chunkLoc;
    }

    public boolean chunkInRange(Chunk chunk)
    {
        if (!world.equals(chunk.getWorld()))
            return false;

        getChunkCoords();
        getLength();

        int difX = Math.abs(chunk.getX() - chunkLoc.getBlockX());
        int difZ = Math.abs(chunk.getZ() - chunkLoc.getBlockZ());

        return difX <= roundedLength && difZ <= roundedLength;
    }

    public int getRoundedLength()
    {
        if (roundedLength == null)
            getLength();
        return roundedLength;
    }

    public int getLength()
    {
        if (length != null)
            return length;
        int xLen = max.getBlockX() - min.getBlockX();
        int zLen = max.getBlockZ() - min.getBlockZ();

        length = 1;

        // Regular door or Portcullis
        if (type.equals(DoorType.DOOR) || type.equals(DoorType.PORTCULLIS))
            length = xLen > zLen ? xLen : zLen;

            // Drawbridge
        else if (type.equals(DoorType.DRAWBRIDGE) && engineSide != null)
        {
            int yLen = Math.abs(max.getBlockY() - min.getBlockY());
            if (engineSide.equals(DoorDirection.NORTH) || engineSide.equals(DoorDirection.SOUTH))
                length = zLen > yLen ? zLen : yLen;
            else
                length = xLen > yLen ? xLen : yLen;
        }

        // Portcullis engine is in the middle and doesn't rotate.
        if (type.equals(DoorType.PORTCULLIS))
            length /= 2;

        roundedLength = length / 16 + 1;

        return length;
    }

    public DoorDirection getLookingDir()
    {
        if (type.equals(DoorType.DRAWBRIDGE) || type.equals(DoorType.PORTCULLIS))
            return engineSide == DoorDirection.NORTH ||
                       engineSide == DoorDirection.SOUTH ? DoorDirection.NORTH : DoorDirection.EAST;

        return engine.getBlockZ() != min.getBlockZ() ? DoorDirection.NORTH :
               engine.getBlockX() != max.getBlockX() ? DoorDirection.EAST  :
               engine.getBlockZ() != max.getBlockZ() ? DoorDirection.SOUTH :
               engine.getBlockX() != min.getBlockX() ? DoorDirection.WEST  : null;
    }

    public long getPowerBlockChunkHash()
    {
        return Util.chunkHashFromLocation(powerBlock.getBlockX(), powerBlock.getBlockZ(), world.getUID());
    }

    private int calculateBlockCount()
    {
        int xLen = Math.abs(getMaximum().getBlockX() - getMinimum().getBlockX()) + 1;
        int yLen = Math.abs(getMaximum().getBlockY() - getMinimum().getBlockY()) + 1;
        int zLen = Math.abs(getMaximum().getBlockZ() - getMinimum().getBlockZ()) + 1;
        xLen = xLen == 0 ? 1 : xLen;
        yLen = yLen == 0 ? 1 : yLen;
        zLen = zLen == 0 ? 1 : zLen;

        return xLen * yLen * zLen;
    }

    public int getBlockCount()
    {
        return blockCount == null ? blockCount = calculateBlockCount() : blockCount;
    }

    public String toSimpleString()
    {
        return doorUID + " \"" + name + "\"";
    }

    @Override
    public String toString()
    {
        String ret = "";
        ret += "\"" + name + "\". Owned by \"" + player.toString() + "\" (" + getPermission() + ")\n";
        ret += "Max = " + Util.locIntToString(max) + ", min = " + Util.locIntToString(min) + "\n";
        ret += "Eng = " + Util.locIntToString(engine) + ", locked = " + isLocked + ", open = " + isOpen + "\n";
        ret += "blockCount = " + getBlockCount() + ", hash = " + getPowerBlockChunkHash();
        return ret;
    }
}
