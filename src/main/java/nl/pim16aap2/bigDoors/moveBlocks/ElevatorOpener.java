package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.World;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class ElevatorOpener implements Opener
{
    private BigDoors plugin;

    DoorDirection ddirection;

    public ElevatorOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    // Check if the chunks at the minimum and maximum locations of the door are loaded.
    private boolean chunksLoaded(Door door)
    {
        // Return true if the chunk at the max and at the min of the chunks were loaded correctly.
        if (door.getWorld() == null)
            plugin.getMyLogger().logMessage("World is null for door \""    + door.getName().toString() + "\"",          true, false);
        if (door.getWorld().getChunkAt(door.getMaximum()) == null)
            plugin.getMyLogger().logMessage("Chunk at maximum for door \"" + door.getName().toString() + "\" is null!", true, false);
        if (door.getWorld().getChunkAt(door.getMinimum()) == null)
            plugin.getMyLogger().logMessage("Chunk at minimum for door \"" + door.getName().toString() + "\" is null!", true, false);

        return door.getWorld().getChunkAt(door.getMaximum()).load() && door.getWorld().getChunkAt(door.getMinimum()).isLoaded();
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
        if (plugin.getCommander().isDoorBusy(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
            return DoorOpenResult.BUSY;
        }

        if (!chunksLoaded(door))
        {
            plugin.getMyLogger().logMessage(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!", true, false);
            return DoorOpenResult.ERROR;
        }

        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = plugin.getConfigLoader().maxDoorSize();
        if (maxDoorSize != -1)
            if(door.getBlockCount() > maxDoorSize)
                instantOpen = true;

        int blocksToMove = getBlocksToMove(door);
        Util.broadcastMessage("blocksToMove = " + blocksToMove);

        if (blocksToMove != 0)
        {
            // Change door availability so it cannot be opened again (just temporarily, don't worry!).
            plugin.getCommander().setDoorBusy(door.getDoorUID());

            plugin.addBlockMover(new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove));
        }
        return DoorOpenResult.SUCCESS;
    }

    private int getBlocksInDir(Door door, RotateDirection upDown)
    {
        int xMin, xMax, zMin, zMax, yMin, yMax, yLen, blocksMoved = 0, step;
        xMin  = door.getMinimum().getBlockX();
        yMin  = door.getMinimum().getBlockY();
        zMin  = door.getMinimum().getBlockZ();
        xMax  = door.getMaximum().getBlockX();
        yMax  = door.getMaximum().getBlockY();
        zMax  = door.getMaximum().getBlockZ();
        yLen  = yMax - yMin + 1;
        int distanceToCheck = door.getBlocksToMove() < 1 ? yLen : door.getBlocksToMove();
        Util.broadcastMessage("DistanceToCheck = " + distanceToCheck + ", yLen = " + yLen);

        int xAxis, yAxis, zAxis, yGoal;
        World world = door.getWorld();
        step  = upDown == RotateDirection.DOWN ? -1 : 1;
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
        Util.broadcastMessage("blocksMoved = " + blocksMoved);
        return blocksMoved;
    }

    private int getBlocksToMove(Door door)
    {
        int blocksUp = 0, blocksDown = 0;
        if (door.getOpenDir() == RotateDirection.UP   && !door.isOpen() ||
            door.getOpenDir() == RotateDirection.DOWN &&  door.isOpen())
            blocksUp    = getBlocksInDir(door, RotateDirection.UP  );
        else
            blocksDown  = getBlocksInDir(door, RotateDirection.DOWN);
        return blocksUp > -1 * blocksDown ? blocksUp : blocksDown;
    }
}
