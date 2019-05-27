package nl.pim16aap2.bigdoors.moveblocks;

import java.util.logging.Level;

import org.bukkit.World;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

public class ElevatorOpener extends Opener
{
    MyBlockFace ddirection;

    public ElevatorOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        if (plugin.getDatabaseManager().isDoorBusy(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
            return DoorOpenResult.BUSY;
        }

        if (!chunksLoaded(door))
        {
            plugin.getMyLogger().logMessage(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!",
                                            true, false);
            return DoorOpenResult.ERROR;
        }

        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = plugin.getConfigLoader().maxDoorSize();
        if (maxDoorSize != -1)
            if (door.getBlockCount() > maxDoorSize)
                instantOpen = true;

        int blocksToMove = getBlocksToMove(door);

        if (blocksToMove != 0)
        {
            // Change door availability so it cannot be opened again (just temporarily,
            // don't worry!).
            plugin.getDatabaseManager().setDoorBusy(door.getDoorUID());

            plugin.addBlockMover(new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove,
                                                   plugin.getConfigLoader().getMultiplier(DoorType.ELEVATOR)));
        }
        return DoorOpenResult.SUCCESS;
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
                    if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis)))
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
        return blocksUp > -1 * blocksDown ? blocksUp : blocksDown;
    }
}
