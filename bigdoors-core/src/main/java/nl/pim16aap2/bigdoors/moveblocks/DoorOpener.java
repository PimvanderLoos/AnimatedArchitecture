package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.DoorOpenResult;
import nl.pim16aap2.bigdoors.spigotutil.Util;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;

public class DoorOpener extends Opener
{
    public DoorOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Check if the block on the north/east/south/west side of the location is free.
    private boolean isPosFree(DoorBase door, MyBlockFace direction, Location newMin, Location newMax)
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
                    if (!Util.isAirOrLiquid(engLoc.getWorld().getBlockAt(xAxis, yAxis, zAxis)))
                        return false;

        newMin.setX(startX);
        newMin.setY(startZ);
        newMin.setZ(startZ);
        newMax.setX(endX);
        newMax.setY(endZ);
        newMax.setZ(endZ);

        return true;
    }

    // Determine which direction the door is going to rotate. Clockwise or
    // counterclockwise.
    private RotateDirection getRotationDirection(DoorBase door, MyBlockFace currentDir, Location newMin, Location newMax)
    {
        RotateDirection openDir = door.getOpenDir();
        openDir = openDir.equals(RotateDirection.CLOCKWISE) && door.isOpen() ? RotateDirection.COUNTERCLOCKWISE :
            openDir.equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen() ? RotateDirection.CLOCKWISE : openDir;
        switch (currentDir)
        {
        case NORTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.EAST, newMin, newMax))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.WEST, newMin, newMax))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case EAST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.SOUTH, newMin, newMax))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.NORTH, newMin, newMax))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case SOUTH:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.WEST, newMin, newMax))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.EAST, newMin, newMax))
                return RotateDirection.COUNTERCLOCKWISE;
            break;

        case WEST:
            if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) && isPosFree(door, MyBlockFace.NORTH, newMin, newMax))
                return RotateDirection.CLOCKWISE;
            else if (!openDir.equals(RotateDirection.CLOCKWISE) && isPosFree(door, MyBlockFace.SOUTH, newMin, newMax))
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
    private MyBlockFace getCurrentDirection(DoorBase door)
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
    public DoorOpenResult openDoor(DoorBase door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return abort(door, isOpenable);
        super.setBusy(door);

        if (super.isTooBig(door))
            instantOpen = true;

        MyBlockFace currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger()
                .warn("Current direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorOpenResult.ERROR);
        }
        Location newMin = new Location(door.getWorld(), 0, 0, 0);
        Location newMax = new Location(door.getWorld(), 0, 0, 0);
        RotateDirection rotDirection = getRotationDirection(door, currentDirection, newMin, newMax);
        if (rotDirection == null)
        {
            plugin.getMyLogger()
                .warn("Rotation direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorOpenResult.NODIRECTION);
        }

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), newMin, newMax) != null)
            return abort(door, DoorOpenResult.NOPERMISSION);

        plugin.addBlockMover(new CylindricalMover(plugin, door.getWorld(), rotDirection, time, currentDirection, door,
                                                  instantOpen, plugin.getConfigLoader().getMultiplier(DoorType.BIGDOOR)));

        return DoorOpenResult.SUCCESS;
    }
}
