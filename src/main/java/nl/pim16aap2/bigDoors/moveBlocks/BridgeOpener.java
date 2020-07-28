package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.Optional;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.ChunkUtils;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadMode;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadResult;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.Pair;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.Vector2D;

public class BridgeOpener implements Opener
{
    private final BigDoors plugin;

    public BridgeOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Vector2D, Vector2D> getChunkRange(Door door)
    {
        if (!door.isOpen())
            return getCurrentChunkRange(door);

        DoorDirection doorDirection = getOpenDirection(door);
        if (doorDirection == null)
        {
            plugin.getMyLogger().warn("Failed to obtain good chunk range for door: " + door.getDoorUID()
                + "! Using current range instead!");
            return getCurrentChunkRange(door);
        }

        return getChunkRange(door, RotateDirection.DOWN, doorDirection);
    }

    private Pair<Vector2D, Vector2D> getChunkRange(Door door, RotateDirection upDown, DoorDirection cardinal)
    {
        Location newMin = new Location(null, 0, 0, 0);
        Location newMax = new Location(null, 0, 0, 0);

        getNewLocations(newMin, newMax, door, upDown, cardinal);
        return ChunkUtils.getChunkRangeBetweenCoords(newMin, newMax);
    }

    private RotateDirection getOppositeAsRotate(final DoorDirection curDir)
    {
        switch (curDir)
        {
        case EAST:
            return RotateDirection.WEST;
        case NORTH:
            return RotateDirection.SOUTH;
        case SOUTH:
            return RotateDirection.NORTH;
        case WEST:
            return RotateDirection.EAST;
        default:
            return RotateDirection.NONE;
        }
    }

    private DoorDirection getOpposite(final DoorDirection curDir)
    {
        switch (curDir)
        {
        case EAST:
            return DoorDirection.WEST;
        case NORTH:
            return DoorDirection.SOUTH;
        case SOUTH:
            return DoorDirection.NORTH;
        case WEST:
            return DoorDirection.EAST;
        }
        throw new IllegalArgumentException("Unable to get opposite direction of: " + curDir.name());
    }

    @Override
    public boolean isRotateDirectionValid(Door door)
    {
        // When rotation point is positioned along the NORTH/SOUTH axis, it can only
        // rotate EAST or WEST. Or the other way round.
        boolean NS = door.getEngSide().equals(DoorDirection.EAST) || door.getEngSide().equals(DoorDirection.WEST);
        if (NS)
            return door.getOpenDir().equals(RotateDirection.EAST) || door.getOpenDir().equals(RotateDirection.WEST);
        return door.getOpenDir().equals(RotateDirection.NORTH) || door.getOpenDir().equals(RotateDirection.SOUTH);
    }

    @Override
    public RotateDirection getRotateDirection(Door door)
    {
        if (isRotateDirectionValid(door))
            return door.getOpenDir();
        return getOppositeAsRotate(door.getEngSide());
    }

    private void getNewLocations(Location min, Location max, Door door, RotateDirection upDown, DoorDirection cardinal)
    {
        int startX = 0, startY = 0, startZ = 0;
        int endX = 0, endY = 0, endZ = 0;
        World world = door.getWorld();

        if (upDown.equals(RotateDirection.UP))
            switch (cardinal)
            {
            // North West = Min X, Min Z
            // South West = Min X, Max Z
            // North East = Max X, Min Z
            // South East = Max X, Max X
            case NORTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMinimum().getBlockZ();
                break;

            case SOUTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

                startZ = door.getMaximum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;

            case EAST:
                startX = door.getMaximum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;

            case WEST:
                startX = door.getMinimum().getBlockX();
                endX = door.getMinimum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;
            }
        else
            switch (cardinal)
            {
            // North West = Min X, Min Z
            // South West = Min X, Max Z
            // North East = Max X, Min Z
            // South East = Max X, Max X
            case NORTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
                endZ = door.getMinimum().getBlockZ() - 1;
                break;

            case SOUTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ() + 1;
                endZ = door.getMinimum().getBlockZ() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();
                break;

            case EAST:
                startX = door.getMinimum().getBlockX() + 1;
                endX = door.getMaximum().getBlockX() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;

            case WEST:
                startX = door.getMinimum().getBlockX() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
                endX = door.getMinimum().getBlockX() - 1;

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
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

    // Check if the new position is free.
    private boolean isNewPosFree(Door door, RotateDirection upDown, DoorDirection cardinal)
    {
        Location newMin = new Location(null, 0, 0, 0);
        Location newMax = new Location(null, 0, 0, 0);
        getNewLocations(newMin, newMax, door, upDown, cardinal);

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

    // Check if the bridge should go up or down.
    public RotateDirection getUpDown(Door door)
    {
        int height = Math.abs(door.getMinimum().getBlockY() - door.getMaximum().getBlockY());
        if (height > 0)
            return RotateDirection.DOWN;
        return RotateDirection.UP;
    }

    private DoorDirection getOpenDirection(Door door)
    {
        if (door.getOpenDir() == null)
            return null;

        RotateDirection upDown = getUpDown(door);
        DoorDirection cDir = getCurrentDirection(door);
        if (upDown == null || cDir == null)
            return null;

        if (upDown.equals(RotateDirection.UP))
            return door.getEngSide();

        if (isRotateDirectionValid(door))
        {
            Optional<DoorDirection> newDir = Util.getDoorDirection(door.getOpenDir())
                .map(dir -> door.isOpen() ? getOpposite(dir) : dir);
            if (newDir.isPresent())
                return newDir.get();
        }

        boolean NS = cDir == DoorDirection.NORTH || cDir == DoorDirection.SOUTH;
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && !door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen())
            return NS ? DoorDirection.SOUTH : DoorDirection.EAST;
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && !door.isOpen())
            return NS ? DoorDirection.NORTH : DoorDirection.WEST;

        if (door.getOpenDir().equals(RotateDirection.NONE) && !door.isOpen())
        {
            if (NS)
                return isNewPosFree(door, upDown, DoorDirection.NORTH) ? DoorDirection.NORTH :
                    isNewPosFree(door, upDown, DoorDirection.SOUTH) ? DoorDirection.SOUTH : null;
            return isNewPosFree(door, upDown, DoorDirection.WEST) ? DoorDirection.WEST :
                isNewPosFree(door, upDown, DoorDirection.EAST) ? DoorDirection.EAST : null;
        }
        return null;
    }

    // Get the "current direction". In this context this means on which side of the
    // drawbridge the engine is.
    private DoorDirection getCurrentDirection(Door door)
    {
        return door.getEngSide();
    }

    @Override
    public DoorOpenResult shadowToggle(Door door)
    {
        if (plugin.getCommander().isDoorBusyRegisterIfNot(door.getDoorUID()))
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Bridge " + door.getName() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }
        DoorDirection openDirection = getOpenDirection(door);
        final RotateDirection upDown = getUpDown(door);
        if (door.getOpenDir().equals(RotateDirection.NONE) || openDirection == null || upDown == null)
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " has no open direction!");
            return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
        }
        BridgeMover.updateCoords(door, openDirection, upDown, 0, true);
        return abort(DoorOpenResult.SUCCESS, door.getDoorUID());
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time)
    {
        return openDoor(door, time, false, false);
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent, ChunkLoadMode mode)
    {
        if (plugin.getCommander().isDoorBusyRegisterIfNot(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO, "Bridge " + door.getName() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }

        final ChunkLoadResult chunkLoadResult = chunksLoaded(door, mode);
        if (chunkLoadResult == ChunkLoadResult.FAIL)
        {
            plugin.getMyLogger().logMessage("Chunks for bridge " + door.getName() + " are not loaded!", true, false);
            return abort(DoorOpenResult.CHUNKSNOTLOADED, door.getDoorUID());
        }
        if (chunkLoadResult == ChunkLoadResult.REQUIRED_LOAD)
            instantOpen = true;

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger()
                .logMessage("Current direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        RotateDirection upDown = getUpDown(door);
        if (upDown == null)
        {
            plugin.getMyLogger()
                .logMessage("UpDown direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        DoorDirection openDirection = getOpenDirection(door);
        if (openDirection == null || !isNewPosFree(door, upDown, openDirection))
        {
            plugin.getMyLogger().logMessage("OpenDirection direction is null for bridge " + door.getName() + " ("
                + door.getDoorUID() + ")!", true, false);
            return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
        }
        if (!isRotateDirectionValid(door))
        {
            // If the door is currently open, then the selected rotDirection is actually the
            // closing direction.
            // So, if the door is open, flip the direciton.
            RotateDirection newRotDir = RotateDirection.valueOf(openDirection.name());
            if (door.isOpen())
            {
                newRotDir = newRotDir.equals(RotateDirection.NORTH) ? RotateDirection.SOUTH :
                    newRotDir.equals(RotateDirection.SOUTH) ? RotateDirection.NORTH :
                    newRotDir.equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
            }

            plugin.getMyLogger().logMessage(
                                            "Updating openDirection of drawbridge " + door.getName() + " to "
                                                + newRotDir.name() + ". If this is undesired, change it via the GUI.",
                                            true, false);
            plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), newRotDir);
        }

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

        plugin.getCommander().addBlockMover(new BridgeMover(plugin, door.getWorld(), time, door, upDown, openDirection,
                                                            instantOpen, plugin.getConfigLoader().dbMultiplier()));
        fireDoorEventToggleStart(door, instantOpen);

        return DoorOpenResult.SUCCESS;
    }
}
