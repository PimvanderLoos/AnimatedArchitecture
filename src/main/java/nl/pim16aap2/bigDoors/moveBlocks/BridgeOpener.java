package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class BridgeOpener implements Opener
{
    private final BigDoors plugin;

    public BridgeOpener(BigDoors plugin)
    {
        this.plugin = plugin;
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

    @Override
    public boolean isRotateDirectionValid(Door door)
    {
        if (door.getOpenDir().equals(RotateDirection.NONE) || door.getOpenDir().equals(RotateDirection.CLOCKWISE) ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) ||
            door.getOpenDir().equals(RotateDirection.UP) || door.getOpenDir().equals(RotateDirection.DOWN))
            return false;

        // When the rotation point is positioned on the NORTH/SOUTH axis,
        // the engine must be either on the EAST or the WEST side.
        boolean NS = door.getEngSide().equals(DoorDirection.EAST) || door.getEngSide().equals(DoorDirection.WEST);
        boolean north = door.getOpenDir().equals(RotateDirection.NORTH);
        boolean east = door.getOpenDir().equals(RotateDirection.EAST);
        boolean south = door.getOpenDir().equals(RotateDirection.SOUTH);
        boolean west = door.getOpenDir().equals(RotateDirection.WEST);

        // When rotation point is positioned along the NORTH/SOUTH axis, it can only
        // rotate
        // EAST or WEST. Or the other way round.
        return ((NS && east) || (NS && west)) || ((!NS && north) || (!NS && south));
    }

    @Override
    public RotateDirection getRotateDirection(Door door)
    {
        if (isRotateDirectionValid(door))
            return door.getOpenDir();
        return getOppositeAsRotate(door.getEngSide());
    }

    // Check if the new position is free.
    private boolean isNewPosFree(Door door, RotateDirection upDown, DoorDirection cardinal)
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

        for (int xAxis = startX; xAxis <= endX; ++xAxis)
            for (int yAxis = startY; yAxis <= endY; ++yAxis)
                for (int zAxis = startZ; zAxis <= endZ; ++zAxis)
                    if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis).getType()))
                        return false;

        door.setNewMin(new Location(door.getWorld(), startX, startY, startZ));
        door.setNewMax(new Location(door.getWorld(), endX, endY, endZ));

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

    private DoorDirection getShadowDirection(Door door)
    {
        if (door.getOpenDir() == null || door.getOpenDir().equals(RotateDirection.NONE))
            return null;

        RotateDirection upDown = getUpDown(door);
        DoorDirection cDir = getCurrentDirection(door);
        if (upDown == null || cDir == null)
            return null;

        boolean NS = cDir == DoorDirection.NORTH || cDir == DoorDirection.SOUTH;

        if (upDown.equals(RotateDirection.UP))
            return door.getEngSide();
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && !door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen())
            return NS ? DoorDirection.SOUTH : DoorDirection.EAST;
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && !door.isOpen())
            return NS ? DoorDirection.NORTH : DoorDirection.WEST;
        return null;
    }

    // Figure out which way the bridge should go.
    private DoorDirection getOpenDirection(Door door)
    {
        RotateDirection upDown = getUpDown(door);
        DoorDirection cDir = getCurrentDirection(door);
        boolean NS = cDir == DoorDirection.NORTH || cDir == DoorDirection.SOUTH;

        if (upDown.equals(RotateDirection.UP))
            return isNewPosFree(door, upDown, door.getEngSide()) ? door.getEngSide() : null;

        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && !door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen())
            return NS && isNewPosFree(door, upDown, DoorDirection.SOUTH) ? DoorDirection.SOUTH :
                !NS && isNewPosFree(door, upDown, DoorDirection.EAST) ? DoorDirection.EAST : null;
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && !door.isOpen())
            return NS && isNewPosFree(door, upDown, DoorDirection.NORTH) ? DoorDirection.NORTH :
                !NS && isNewPosFree(door, upDown, DoorDirection.WEST) ? DoorDirection.WEST : null;

        return NS && isNewPosFree(door, upDown, DoorDirection.NORTH) ? DoorDirection.NORTH :
            !NS && isNewPosFree(door, upDown, DoorDirection.EAST) ? DoorDirection.EAST :
            NS && isNewPosFree(door, upDown, DoorDirection.SOUTH) ? DoorDirection.SOUTH :
            !NS && isNewPosFree(door, upDown, DoorDirection.WEST) ? DoorDirection.WEST : null;
    }

    // Get the "current direction". In this context this means on which side of the
    // drawbridge the engine is.
    private DoorDirection getCurrentDirection(Door door)
    {
        return door.getEngSide();
    }

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
        if (plugin.getCommander().isDoorBusy(door.getDoorUID()))
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Bridge " + door.getName() + " is not available right now!");
            return DoorOpenResult.BUSY;
        }
        DoorDirection openDirection = getShadowDirection(door);
        final RotateDirection upDown = getUpDown(door);
        if (door.getOpenDir().equals(RotateDirection.NONE) || openDirection == null || upDown == null)
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " has no open direction!");
            return DoorOpenResult.NODIRECTION;
        }
        BridgeMover.updateCoords(door, openDirection, upDown, 0, true);
        return DoorOpenResult.SUCCESS;
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time)
    {
        return openDoor(door, time, false, false);
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        if (plugin.getCommander().isDoorBusy(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO, "Bridge " + door.getName() + " is not available right now!");
            return DoorOpenResult.BUSY;
        }

        if (!chunksLoaded(door))
        {
            plugin.getMyLogger().logMessage(ChatColor.RED + "Chunk for bridge " + door.getName() + " is not loaded!",
                                            true, false);
            return DoorOpenResult.ERROR;
        }

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger()
                .logMessage("Current direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return DoorOpenResult.ERROR;
        }

        RotateDirection upDown = getUpDown(door);
        if (upDown == null)
        {
            plugin.getMyLogger()
                .logMessage("UpDown direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return DoorOpenResult.ERROR;
        }

        DoorDirection openDirection = getOpenDirection(door);
        if (openDirection == null)
        {
            plugin.getMyLogger().logMessage("OpenDirection direction is null for bridge " + door.getName() + " ("
                + door.getDoorUID() + ")!", true, false);
            return DoorOpenResult.NODIRECTION;
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
                                            "Updating openDirection of sliding door " + door.getName() + " to "
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
            return DoorOpenResult.ERROR;
        }

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(), door.getNewMin(), door.getNewMax()) != null ||
            plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(), door.getMinimum(), door.getMinimum()) != null)
            return DoorOpenResult.NOPERMISSION;

        if (fireDoorEventTogglePrepare(door, instantOpen))
            return DoorOpenResult.CANCELLED;

        // Change door availability so it cannot be opened again (just temporarily,
        // don't worry!).
        plugin.getCommander().setDoorBusy(door.getDoorUID());
        plugin.getCommander().addBlockMover(new BridgeMover(plugin, door.getWorld(), time, door, upDown, openDirection,
                                                            instantOpen, plugin.getConfigLoader().dbMultiplier()));
        fireDoorEventToggleStart(door, instantOpen);

        return DoorOpenResult.SUCCESS;
    }
}
