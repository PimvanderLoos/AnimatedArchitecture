package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.ChunkUtils;
import nl.pim16aap2.bigDoors.util.ChunkUtils.Mode;
import nl.pim16aap2.bigDoors.util.ChunkUtils.Result;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.Pair;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.Vector2D;

public class DoorOpener implements Opener
{
    private final BigDoors plugin;

    public DoorOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Vector2D, Vector2D> getChunkRange(Door door)
    {
        return getChunkRange(door, getNewDirection(door));
    }

    private Pair<Vector2D, Vector2D> getChunkRange(Door door, DoorDirection newDirection)
    {
        if (newDirection == null)
        {
            plugin.getMyLogger().warn("Failed to obtain good chunk range for door: " + door.getDoorUID()
                + "! Using current range instead!");
            return getCurrentChunkRange(door);
        }

        Location newMin = new Location(null, 0, 0, 0);
        Location newMax = new Location(null, 0, 0, 0);

        getNewLocations(newMin, newMax, door, newDirection);
        return getChunkRange(door, newMin, newMax);
    }

    private Pair<Vector2D, Vector2D> getChunkRange(Door door, Location newMin, Location newMax)
    {
        return ChunkUtils.getChunkRangeBetweenSortedCoords(newMin, door.getMinimum(), newMax, door.getMaximum());
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

        RotateDirection rotateDir = getRotationDirection(door, getCurrentDirection(door));
        return rotateDir != null ? rotateDir : RotateDirection.CLOCKWISE;
    }

    private void getNewLocations(Location min, Location max, Door door, DoorDirection newDirection)
    {
        Location engLoc = door.getEngine();
        int startX = engLoc.getBlockX();
        int startY = door.getMinimum().getBlockY();
        int startZ = engLoc.getBlockZ();

        int endX = engLoc.getBlockX();
        int endY = door.getMaximum().getBlockY();
        int endZ = engLoc.getBlockZ();

        int xLen = door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
        int zLen = door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();
        World world = door.getWorld();

        switch (newDirection)
        {
        case NORTH:
            startZ = engLoc.getBlockZ() - xLen;
            endZ = engLoc.getBlockZ() - 1;
            break;
        case EAST:
            startX = engLoc.getBlockX() + 1;
            endX = engLoc.getBlockX() + zLen;
            endZ = engLoc.getBlockZ();
            break;
        case SOUTH:
            startZ = engLoc.getBlockZ() + 1;
            endZ = engLoc.getBlockZ() + xLen;
            break;
        case WEST:
            startX = engLoc.getBlockX() - zLen;
            endX = engLoc.getBlockX() - 1;
            break;
        }

        min.setWorld(world);
        min.setX(startX);
        min.setY(startY);
        min.setZ(startZ);

        max.setWorld(world);
        max.setX(endX);
        max.setY(endY);
        max.setZ(endZ);
    }

    // Check if the block on the north/east/south/west side of the location is free.
    private boolean isPosFree(Door door, DoorDirection direction)
    {
        Location newMin = new Location(null, 0, 0, 0);
        Location newMax = new Location(null, 0, 0, 0);
        getNewLocations(newMin, newMax, door, direction);

        World world = door.getWorld();
        for (int xAxis = newMin.getBlockX(); xAxis <= newMax.getBlockX(); ++xAxis)
            for (int yAxis = newMin.getBlockY(); yAxis <= newMax.getBlockY(); ++yAxis)
                for (int zAxis = newMin.getBlockZ(); zAxis <= newMax.getBlockZ(); ++zAxis)
                    if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis).getType()))
                        return false;

        door.setNewMin(newMin);
        door.setNewMax(newMax);
        return true;
    }

    private DoorDirection getNewDirection(Door door)
    {
        DoorDirection currentDir = getCurrentDirection(door);
        RotateDirection openDir = door.getOpenDir();
        openDir = openDir.equals(RotateDirection.CLOCKWISE) && door.isOpen() ? RotateDirection.COUNTERCLOCKWISE :
            openDir.equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen() ? RotateDirection.CLOCKWISE : openDir;

        if (!isRotateDirectionValid(door))
            return null;

        switch (currentDir)
        {
        case NORTH:
            return openDir.equals(RotateDirection.COUNTERCLOCKWISE) ? DoorDirection.EAST : DoorDirection.WEST;

        case EAST:
            return openDir.equals(RotateDirection.COUNTERCLOCKWISE) ? DoorDirection.NORTH : DoorDirection.SOUTH;

        case SOUTH:
            return openDir.equals(RotateDirection.COUNTERCLOCKWISE) ? DoorDirection.WEST : DoorDirection.EAST;

        case WEST:
            return openDir.equals(RotateDirection.COUNTERCLOCKWISE) ? DoorDirection.SOUTH : DoorDirection.NORTH;
        }
        return null;
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

        return abort(DoorOpenResult.SUCCESS, door.getDoorUID());
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time)
    {
        return openDoor(door, time, false, false);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent, Mode mode)
    {
        if (plugin.getCommander().isDoorBusyRegisterIfNot(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }

        final Result chunkLoadResult = chunksLoaded(door, mode);
        if (chunkLoadResult == Result.FAIL)
        {
            plugin.getMyLogger().logMessage("Chunks for door " + door.getName() + " are not loaded!", true, false);
            return abort(DoorOpenResult.CHUNKSNOTLOADED, door.getDoorUID());
        }
        if (chunkLoadResult == Result.REQUIRED_LOAD)
            instantOpen = true;

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
