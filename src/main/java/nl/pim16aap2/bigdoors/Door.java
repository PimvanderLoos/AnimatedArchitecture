package nl.pim16aap2.bigdoors;

import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.util.DoorDirection;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

/* "Rewrite" todo list

- Get rid of the commander.
- Get rid of the command handler.
- Give every GUI page its own class.
- Rewrite all block movers etc.
  - When rewriting, make sure that absolutely 0 implementation-specific code ends up in the new movers.
- Use Maven modules to be able to support multiple versions and perhaps even Forge / Sponge / whatever.
- Clean up SQL class. Try to move as many shared items to private classes and/or use single statements.
- Allow 1-wide drawbridges. Finally.
 */


public class Door
{
    private Location min;
    private Location max;
    private String name;
    private DoorType type;
    private World world;
    private boolean canGo;
    private boolean isOpen;
    private Location engine;
    private UUID player;
    private final long doorUID;
    private RotateDirection openDir;
    private boolean isLocked;
    private int autoClose;
    private DoorDirection engineSide;
    private Location powerBlock;
    private Integer roundedLength;
    private int permission;
    private Location chunkLoc;
    private Integer length;
    private Location newMin;
    private Location newMax;

    private int blocksToMove = 0;

    // Generate a new door.
    public Door(UUID player, World world, Location min, Location max, Location engine, String name, boolean isOpen,
        long doorUID, boolean isLocked, int permission, DoorType type, DoorDirection engineSide, Location powerBlock,
        RotateDirection openDir, int autoClose)
    {
        this.player = player;
        this.world = world;
        this.min = min;
        this.max = max;
        this.engine = engine;
        this.powerBlock = powerBlock;
        this.name = name;
        this.isOpen = isOpen;
        this.doorUID = doorUID;
        this.isLocked = isLocked;
        this.permission = permission;
        this.type = type;
        this.engineSide = engineSide;
        chunkLoc = null;
        length = null;
        roundedLength = null;
        canGo = true;
        this.openDir = openDir == null ? RotateDirection.NONE : openDir;
        this.autoClose = autoClose;
    }

    public Door(UUID player, World world, Location min, Location max, Location engine, String name, boolean isOpen,
        long doorUID, boolean isLocked, int permission, DoorType type, Location powerBlock, RotateDirection openDir,
        int autoClose)
    {
        this(player, world, min, max, engine, name, isOpen, doorUID, isLocked, permission, type, null, powerBlock,
             openDir, autoClose);
    }

    // Create a door with a player UUID string instead of player Object.
    public Door(World world, Location min, Location max, Location engine, String name, boolean isOpen, long doorUID,
        boolean isLocked, int permission, String player, DoorType type, Location powerBlock, RotateDirection openDir,
        int autoClose)
    {
        this(UUID.fromString(player), world, min, max, engine, name, isOpen, doorUID, isLocked, permission, type, null,
             powerBlock, openDir, autoClose);
    }

    // Create a door with a player UUID string instead of player Object and an
    // engineSide (for draw bridges).
    public Door(World world, Location min, Location max, Location engine, String name, boolean isOpen, long doorUID,
        boolean isLocked, int permission, String player, DoorType type, DoorDirection engineSide, Location powerBlock,
        RotateDirection openDir, int autoClose)
    {
        this(UUID.fromString(player), world, min, max, engine, name, isOpen, doorUID, isLocked, permission, type,
             engineSide, powerBlock, openDir, autoClose);
    }

    // ------------------------ SIMPLE GETTERS -------------------- //
    public DoorType getType()           {  return type;          }  // Get this door's type.
    public String getName()             {  return name;          }  // Get the name of the door.
    public World getWorld()             {  return world;         }  // Get the world this door is in.
    public long getDoorUID()            {  return doorUID;       }  // Get doorUID as used in the doors table in the db.
    public boolean isLocked()           {  return isLocked;      }  // Check if this door is locked or not.
    public boolean isOpen()             {  return isOpen;        }  // Check if this door is in the open or closed state.
    public int getPermission()          {  return permission;    }  // Check permission level of current owner.
    public UUID getPlayerUUID()         {  return player;        }  // Get UUID of the owner of the door. Might be null!
    public DoorDirection getEngSide()   {  return engineSide;    }  // Get this door's (or drawbridge's, in this case) engine side.
    public boolean canGo()              {  return canGo;         }  // Check if this door can still be opened or not.
    public RotateDirection getOpenDir() {  return openDir;       }  // Get the open direction of this door.
    public int getAutoClose()           {  return autoClose;     }  // Get the auto close time.
    public int getBlocksToMove()        {  return blocksToMove;  }  // Get the desired number of blocks to move this door.

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

    public void setCanGo(boolean bool)
    {
        canGo = bool;
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

        Chunk chunk = world.getBlockAt((int) engine.getX(), (int) engine.getY(),
                                              (int) engine.getZ()).getChunk();
        chunkLoc = new Location(world, chunk.getX(), 0, chunk.getZ());

        return chunkLoc;
    }

    public boolean chunkInRange(Chunk chunk)
    {
        getChunkCoords();
        getLength();

        int deltaX = Math.abs(chunk.getX() - chunkLoc.getBlockX());
        int deltaZ = Math.abs(chunk.getZ() - chunkLoc.getBlockZ());

        return deltaX <= roundedLength && deltaZ <= roundedLength;
    }

    public int getLength()
    {
        if (length != null)
            return length;
        int xLen = Math.abs(max.getBlockX() - min.getBlockX());
        int zLen = Math.abs(max.getBlockZ() - min.getBlockZ());

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

    public Location getNewMin()
    {
        return newMin;
    }

    public Location getNewMax()
    {
        return newMax;
    }

    public void setNewMin(Location loc)
    {
        newMin = loc;
    }

    public void setNewMax(Location loc)
    {
        newMax = loc;
    }

    public int getBlockCount()
    {
        int xLen = Math.abs(getMaximum().getBlockX() - getMinimum().getBlockX()) + 1;
        int yLen = Math.abs(getMaximum().getBlockY() - getMinimum().getBlockY()) + 1;
        int zLen = Math.abs(getMaximum().getBlockZ() - getMinimum().getBlockZ()) + 1;
        xLen = xLen == 0 ? 1 : xLen;
        yLen = yLen == 0 ? 1 : yLen;
        zLen = zLen == 0 ? 1 : zLen;

        return xLen * yLen * zLen;
    }

    public String getBasicInfo()
    {
        return doorUID + " (" + getPermission() + ")" + ": " + name.toString();
    }

    public String getFullInfo()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(doorUID + ": " + name + "\n");
        builder.append("Type: " + type.toString() + ". Permission: " + permission + "\n");
        builder.append("Min: " + Util.locIntToString(min) + ", Max: " + Util.locIntToString(max) + ", Engine: " + Util.locIntToString(engine) + "\n");
        builder.append("PowerBlock location: " + Util.locIntToString(powerBlock) + ". Hash: " + getPowerBlockChunkHash() + "\n");
        builder.append("This door is " + (isLocked ? "" : "NOT ") + "locked. ");
        builder.append("This door is " + (isOpen ? "" : "NOT ") + "open.\n");
        builder.append("OpenDir: " + openDir.toString() + "; Looking: " + getLookingDir() + "\n");

        if (engineSide != null)
            builder.append("EngineSide: " + engineSide.toString() + ". door Length: " + getLength() + "\n");
        builder.append("AutoClose: " + autoClose);

        return builder.toString();
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

