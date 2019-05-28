package nl.pim16aap2.bigdoors.moveblocks;

import org.bukkit.Location;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

public class DoorOpener extends Opener
{
    public DoorOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Check if the block on the north/east/south/west side of the location is free.
    private boolean isPosFree(Door door, MyBlockFace direction)
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
        default:
            plugin.getMyLogger().dumpStackTrace("Invalid direction for door opener: " + direction.toString());
            break;
        }

        for (int xAxis = startX; xAxis <= endX; ++xAxis)
            for (int yAxis = startY; yAxis <= endY; ++yAxis)
                for (int zAxis = startZ; zAxis <= endZ; ++zAxis)
                    if (!Util.isAirOrWater(engLoc.getWorld().getBlockAt(xAxis, yAxis, zAxis)))
                        return false;
        door.setNewMin(new Location(door.getWorld(), startX, startY, startZ));
        door.setNewMax(new Location(door.getWorld(), endX, endY, endZ));

        return true;
    }

    // Determine which direction the door is going to rotate. Clockwise or
    // counterclockwise.
    private RotateDirection getRotationDirection(Door door, MyBlockFace currentDir)
    {
        RotateDirection openDir = door.getOpenDir();
        openDir = openDir.equals(RotateDirection.CLOCKWISE) && door.isOpen() ? RotateDirection.COUNTERCLOCKWISE :
            openDir.equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen() ? RotateDirection.CLOCKWISE : openDir;
        switch (currentDir)
        {
        case NORTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.EAST))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.WEST))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case EAST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.SOUTH))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.NORTH))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case SOUTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.WEST))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.EAST))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case WEST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.NORTH))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.SOUTH))
                return RotateDirection.COUNTERCLOCKWISE;
            break;
        default:
            plugin.getMyLogger().dumpStackTrace("Invalid currentDir for door opener: " + currentDir.toString());
            break;
        }
        return null;
    }

    // Get the direction the door is currently facing as seen from the engine to the
    // end of the door.
    private MyBlockFace getCurrentDirection(Door door)
    {
        // MinZ != EngineZ => Pointing North
        // MaxX != EngineX => Pointing East
        // MaxZ != EngineZ => Pointing South
        // MinX != EngineX => Pointing West
        return door.getEngine().getBlockZ() != door.getMinimum().getBlockZ() ? MyBlockFace.NORTH :
            door.getEngine().getBlockX() != door.getMaximum().getBlockX() ? MyBlockFace.EAST :
            door.getEngine().getBlockZ() != door.getMaximum().getBlockZ() ? MyBlockFace.SOUTH :
            door.getEngine().getBlockX() != door.getMinimum().getBlockX() ? MyBlockFace.WEST : null;
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return isOpenable;
        super.setBusy(door);

        if (super.isTooBig(door))
            instantOpen = true;

        MyBlockFace currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger()
                .logMessage("Current direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return DoorOpenResult.ERROR;
        }

        RotateDirection rotDirection = getRotationDirection(door, currentDirection);
        if (rotDirection == null)
        {
            plugin.getMyLogger()
                .logMessage("Rotation direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return DoorOpenResult.NODIRECTION;
        }

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getNewMin(), door.getNewMax()) != null)
            return DoorOpenResult.NOPERMISSION;

        plugin.addBlockMover(new CylindricalMover(plugin, door.getWorld(), rotDirection, time, currentDirection, door,
                                                  instantOpen, plugin.getConfigLoader().getMultiplier(DoorType.DOOR)));

        return DoorOpenResult.SUCCESS;
    }
}
