package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.ChunkUtils.Mode;
import nl.pim16aap2.bigDoors.util.ChunkUtils.Result;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.Pair;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.Vector2D;

public class ElevatorOpener implements Opener
{
    private final BigDoors plugin;

    DoorDirection ddirection;

    public ElevatorOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Vector2D, Vector2D> getChunkRange(Door door)
    {
        return getCurrentChunkRange(door);
    }

    @Override
    public boolean isRotateDirectionValid(Door door)
    {
        return door.getOpenDir().equals(RotateDirection.UP) || door.getOpenDir().equals(RotateDirection.DOWN);
    }

    @Override
    public RotateDirection getRotateDirection(Door door)
    {
        if (isRotateDirectionValid(door))
            return door.getOpenDir();
        return RotateDirection.UP;
    }

    @Override
    public DoorOpenResult shadowToggle(Door door)
    {
        if (plugin.getCommander().isDoorBusyRegisterIfNot(door.getDoorUID()))
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }
        if (door.getOpenDir().equals(RotateDirection.NONE))
        {
            plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " has no open direction!");
            return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
        }

        // For the blocks to shadow move the door, prefer to use the BTM variable, but
        // use the height if that's unavailable.
        int moved = door.getBlocksToMove() > 0 ? door.getBlocksToMove() :
            (door.getMaximum().getBlockY() - door.getMinimum().getBlockY() + 1);
        int multiplier = door.getOpenDir().equals(RotateDirection.UP) ? 1 : -1;
        multiplier *= door.isOpen() ? 1 : -1;

        VerticalMover.updateCoords(door, null, null, moved * multiplier, true);
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

        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = getSizeLimit(door);
        if (maxDoorSize > 0 && door.getBlockCount() > maxDoorSize)
        {
            plugin.getMyLogger().logMessage("Door \"" + door.getDoorUID() + "\" Exceeds the size limit: " + maxDoorSize,
                                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        int blocksToMove = getBlocksToMove(door);
        if (blocksToMove > BigDoors.get().getConfigLoader().getMaxBlocksToMove())
        {
            plugin.getMyLogger().logMessage("Door \"" + door.getDoorUID() + "\" Exceeds blocksToMove limit: "
                + blocksToMove + ". Limit = " + BigDoors.get().getConfigLoader().getMaxBlocksToMove(), true, false);
            return abort(DoorOpenResult.BLOCKSTOMOVEINVALID, door.getDoorUID());
        }

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(), door.getWorld(),
                                             door.getNewMin(), door.getNewMax()) != null ||
            plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(), door.getWorld(),
                                             door.getMinimum(), door.getMinimum()) != null)
            return abort(DoorOpenResult.NOPERMISSION, door.getDoorUID());

        if (blocksToMove != 0)
        {
            if (!isRotateDirectionValid(door))
            {
                RotateDirection openDirection = door.isOpen() ?
                    (blocksToMove > 0 ? RotateDirection.DOWN : RotateDirection.UP) :
                    (blocksToMove > 0 ? RotateDirection.UP : RotateDirection.DOWN);
                plugin.getMyLogger().logMessage("Updating openDirection of elevator " + door.getName() + " to "
                    + openDirection.name() + ". If this is undesired, change it via the GUI.", true, false);
                plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), openDirection);
            }

            if (fireDoorEventTogglePrepare(door, instantOpen))
                return abort(DoorOpenResult.CANCELLED, door.getDoorUID());

            plugin.getCommander()
                .addBlockMover(new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove,
                                                 plugin.getConfigLoader().elMultiplier()));
            fireDoorEventToggleStart(door, instantOpen);
            return DoorOpenResult.SUCCESS;
        }
        return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
    }

    private int getBlocksInDir(Door door, RotateDirection upDown)
    {
        int xMin, xMax, zMin, zMax, yMin, yMax, yLen, blocksMoved = 0, step;
        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();
        yLen = yMax - yMin + 1;
        int distanceToCheck = door.getBlocksToMove() < 1 ? yLen : door.getBlocksToMove();

        int xAxis, yAxis, zAxis, yGoal;
        World world = door.getWorld();
        step = upDown == RotateDirection.DOWN ? -1 : 1;
        yAxis = upDown == RotateDirection.DOWN ? yMin - 1 : yMax + 1;
        yGoal = upDown == RotateDirection.DOWN ? yMin - distanceToCheck - 1 : yMax + distanceToCheck + 1;

        while (yAxis != yGoal)
        {
            for (xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (zAxis = zMin; zAxis <= zMax; ++zAxis)
                    if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis).getType()))
                        return blocksMoved;
            yAxis += step;
            blocksMoved += step;
        }
        return blocksMoved;
    }

    private int getBlocksToMove(Door door)
    {
        int blocksUp = 0, blocksDown = 0;
        if (door.getOpenDir() == RotateDirection.UP && !door.isOpen() ||
            door.getOpenDir() == RotateDirection.DOWN && door.isOpen())
            blocksUp = getBlocksInDir(door, RotateDirection.UP);
        else
            blocksDown = getBlocksInDir(door, RotateDirection.DOWN);
        int blocksToMove = blocksUp > -1 * blocksDown ? blocksUp : blocksDown;
        door.setNewMin(new Location(door.getWorld(), door.getMinimum().getBlockX(),
                                    door.getMinimum().getBlockY() + blocksToMove, door.getMinimum().getBlockZ()));
        door.setNewMax(new Location(door.getWorld(), door.getMaximum().getBlockX(),
                                    door.getMaximum().getBlockY() + blocksToMove, door.getMaximum().getBlockZ()));
        return blocksToMove;
    }
}
