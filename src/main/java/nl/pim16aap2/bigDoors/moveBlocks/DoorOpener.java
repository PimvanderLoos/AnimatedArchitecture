package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class DoorOpener implements Opener
{
    private final BigDoors plugin;

    public DoorOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean isRotateDirectionValid(Door door)
    {
        return door.getOpenDir().equals(RotateDirection.CLOCKWISE) ||
               door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE);
    }

    @Override
    public RotateDirection getRotateDirection(Door door)
    {
        if (isRotateDirectionValid(door))
            return door.getOpenDir();

        if (!chunksLoaded(door))
            return RotateDirection.NONE;

        RotateDirection rotateDir = getRotationDirection(door, getCurrentDirection(door));
        return rotateDir != null ? rotateDir : RotateDirection.CLOCKWISE;
    }

    // Check if the block on the north/east/south/west side of the location is free.
    private boolean isPosFree(Door door, DoorDirection direction)
    {
        Location engLoc = door.getEngine();
        int endX = 0, endY = 0, endZ = 0;
        int startX = 0, startY = 0, startZ = 0;
        int xLen = door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
        int zLen = door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

        switch (direction)
        {
        case NORTH:
            startX = engLoc.getBlockX();
            startY = engLoc.getBlockY();
            startZ = engLoc.getBlockZ() - xLen;
            endX = engLoc.getBlockX();
            endY = door.getMaximum().getBlockY();
            endZ = engLoc.getBlockZ() - 1;
            break;
        case EAST:
            startX = engLoc.getBlockX() + 1;
            startY = engLoc.getBlockY();
            startZ = engLoc.getBlockZ();
            endX = engLoc.getBlockX() + zLen;
            endY = door.getMaximum().getBlockY();
            endZ = engLoc.getBlockZ();
            break;
        case SOUTH:
            startX = engLoc.getBlockX();
            startY = engLoc.getBlockY();
            startZ = engLoc.getBlockZ() + 1;
            endX = engLoc.getBlockX();
            endY = door.getMaximum().getBlockY();
            endZ = engLoc.getBlockZ() + xLen;
            break;
        case WEST:
            startX = engLoc.getBlockX() - zLen;
            startY = engLoc.getBlockY();
            startZ = engLoc.getBlockZ();
            endX = engLoc.getBlockX() - 1;
            endY = door.getMaximum().getBlockY();
            endZ = engLoc.getBlockZ();
            break;
        }

        for (int xAxis = startX; xAxis <= endX; ++xAxis)
            for (int yAxis = startY; yAxis <= endY; ++yAxis)
                for (int zAxis = startZ; zAxis <= endZ; ++zAxis)
                    if (!Util.isAirOrWater(engLoc.getWorld().getBlockAt(xAxis, yAxis, zAxis).getType()))
                        return false;
        door.setNewMin(new Location(door.getWorld(), startX, startY, startZ));
        door.setNewMax(new Location(door.getWorld(), endX, endY, endZ));

        return true;
    }

    // Determine which direction the door is going to rotate. Clockwise or
    // counterclockwise.
    private RotateDirection getRotationDirection(Door door, DoorDirection currentDir)
    {
        RotateDirection openDir = door.getOpenDir();
        openDir = openDir.equals(RotateDirection.CLOCKWISE) && door.isOpen() ? RotateDirection.COUNTERCLOCKWISE :
            openDir.equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen() ? RotateDirection.CLOCKWISE : openDir;
        switch (currentDir)
        {
        case NORTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, DoorDirection.EAST))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, DoorDirection.WEST))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case EAST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, DoorDirection.SOUTH))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, DoorDirection.NORTH))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case SOUTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, DoorDirection.WEST))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, DoorDirection.EAST))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case WEST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, DoorDirection.NORTH))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, DoorDirection.SOUTH))
                return RotateDirection.COUNTERCLOCKWISE;
            break;
        }
        return null;
    }

    // Determine which direction the door is going to rotate. Clockwise or
    // counterclockwise.
    private RotateDirection getShadowRotationDirection(Door door, DoorDirection currentDir)
    {
        RotateDirection openDir = door.getOpenDir();
        openDir = openDir.equals(RotateDirection.CLOCKWISE) && door.isOpen() ? RotateDirection.COUNTERCLOCKWISE :
            openDir.equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen() ? RotateDirection.CLOCKWISE : openDir;
        switch (currentDir)
        {
        case NORTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case EAST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case SOUTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case WEST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE))
                return RotateDirection.COUNTERCLOCKWISE;
            break;
        }
        return null;
    }

    // Get the direction the door is currently facing as seen from the engine to the
    // end of the door.
    private DoorDirection getCurrentDirection(Door door)
    {
        // MinZ != EngineZ => North
        // MaxX != EngineX => East
        // MaxZ != EngineZ => South
        // MinX != EngineX => West
        return door.getEngine().getBlockZ() != door.getMinimum().getBlockZ() ? DoorDirection.NORTH :
            door.getEngine().getBlockX() != door.getMaximum().getBlockX() ? DoorDirection.EAST :
            door.getEngine().getBlockZ() != door.getMaximum().getBlockZ() ? DoorDirection.SOUTH :
            door.getEngine().getBlockX() != door.getMinimum().getBlockX() ? DoorDirection.WEST : null;
    }

    // Check if the chunks at the minimum and maximum locations of the door are
    // loaded.
    private boolean chunksLoaded(Door door)
    {
        // Return true if the chunk at the max and at the min of the chunks were loaded
        // correctly.
        if (door.getWorld() == null)
            plugin.getMyLogger().logMessage("World is null for door \"" + door.getName().toString() + "\"", true,
                                            false);
        if (door.getWorld().getChunkAt(door.getMaximum()) == null)
            plugin.getMyLogger().logMessage("Chunk at maximum for door \"" + door.getName().toString() + "\" is null!",
                                            true, false);
        if (door.getWorld().getChunkAt(door.getMinimum()) == null)
            plugin.getMyLogger().logMessage("Chunk at minimum for door \"" + door.getName().toString() + "\" is null!",
                                            true, false);

        return door.getWorld().getChunkAt(door.getMaximum()).load() &&
               door.getWorld().getChunkAt(door.getMinimum()).isLoaded();
    }

    @Override
    public DoorOpenResult shadowToggle(Door door)
    {
        if (plugin.getCommander().isDoorBusyRegisterIfNot(door.getDoorUID()))
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }
        DoorDirection currentDirection = getCurrentDirection(door);
        RotateDirection rotDirection = getShadowRotationDirection(door, currentDirection);
        if (door.getOpenDir().equals(RotateDirection.NONE) || currentDirection == null || rotDirection == null)
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " has no open direction!");
            return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
        }
        CylindricalMover.updateCoords(door, currentDirection, rotDirection, 0, true);

        return DoorOpenResult.SUCCESS;
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time)
    {
        return openDoor(door, time, false, false);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        if (plugin.getCommander().isDoorBusyRegisterIfNot(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }

        if (!chunksLoaded(door))
        {
            plugin.getMyLogger().logMessage(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!",
                                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger()
                .logMessage("Current direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        RotateDirection rotDirection = getRotationDirection(door, currentDirection);
        if (rotDirection == null)
        {
            plugin.getMyLogger()
                .logMessage("Rotation direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
        }

        if (!isRotateDirectionValid(door))
        {
            // If the door is currently open, then the selected rotDirection is actually the
            // closing direction.
            // So, if the door is open, flip the direciton.
            RotateDirection newRotDirection = rotDirection;
            if (door.isOpen())
                newRotDirection = newRotDirection.equals(RotateDirection.CLOCKWISE) ? RotateDirection.COUNTERCLOCKWISE :
                    RotateDirection.CLOCKWISE;

            plugin.getMyLogger().logMessage("Updating openDirection of door " + door.getName() + " to "
                + newRotDirection.name() + ". If this is undesired, change it via the GUI.", true, false);
            plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), newRotDirection);
        }

        int xOpposite, yOpposite, zOpposite;
        // If the xMax is not the same value as the engineX, then xMax is xOpposite.
        if (door.getMaximum().getBlockX() != door.getEngine().getBlockX())
            xOpposite = door.getMaximum().getBlockX();
        else
            xOpposite = door.getMinimum().getBlockX();

        // If the zMax is not the same value as the engineZ, then zMax is zOpposite.
        if (door.getMaximum().getBlockZ() != door.getEngine().getBlockZ())
            zOpposite = door.getMaximum().getBlockZ();
        else
            zOpposite = door.getMinimum().getBlockZ();

        // If the yMax is not the same value as the engineY, then yMax is yOpposite.
        if (door.getMaximum().getBlockY() != door.getEngine().getBlockY())
            yOpposite = door.getMaximum().getBlockY();
        else
            yOpposite = door.getMinimum().getBlockY();

        // Finalise the oppositePoint location.
        Location oppositePoint = new Location(door.getWorld(), xOpposite, yOpposite, zOpposite);

        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = getSizeLimit(door);
        if (maxDoorSize > 0 && door.getBlockCount() > maxDoorSize)
        {
            plugin.getMyLogger().logMessage("Door \"" + door.getDoorUID() + "\" Exceeds the size limit: " + maxDoorSize,
                                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(), door.getWorld(),
                                             door.getNewMin(), door.getNewMax()) != null ||
            plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(), door.getWorld(),
                                             door.getMinimum(), door.getMinimum()) != null)
            return abort(DoorOpenResult.NOPERMISSION, door.getDoorUID());

        if (fireDoorEventTogglePrepare(door, instantOpen))
            return abort(DoorOpenResult.CANCELLED, door.getDoorUID());

        plugin.getCommander()
            .addBlockMover(new CylindricalMover(plugin, oppositePoint.getWorld(), 1, rotDirection, time, oppositePoint,
                                                currentDirection, door, instantOpen,
                                                plugin.getConfigLoader().bdMultiplier()));
        fireDoorEventToggleStart(door, instantOpen);

        return DoorOpenResult.SUCCESS;
    }
}
