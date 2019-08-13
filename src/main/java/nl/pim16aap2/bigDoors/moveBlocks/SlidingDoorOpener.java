package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class SlidingDoorOpener implements Opener
{
    private BigDoors plugin;

    public SlidingDoorOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean isRotateDirectionValid(Door door)
    {
        return door.getOpenDir().equals(RotateDirection.NORTH) ||
               door.getOpenDir().equals(RotateDirection.EAST ) ||
               door.getOpenDir().equals(RotateDirection.SOUTH) ||
               door.getOpenDir().equals(RotateDirection.WEST );
    }

    @Override
    public RotateDirection getRotateDirection(Door door)
    {
        if (isRotateDirectionValid(door))
            return door.getOpenDir();

        if (!chunksLoaded(door))
            return RotateDirection.NONE;

        // First try to use the existing methods to figure out where to go. If that doens't work go NORTH if
        RotateDirection openDir = getBlocksToMove(door).getRotateDirection();
        if (openDir.equals(RotateDirection.NONE))
            return door.getMinimum().getBlockX() == door.getMaximum().getBlockX() ? RotateDirection.NORTH : RotateDirection.EAST;
        return openDir;
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

        MovementSpecification blocksToMove = getBlocksToMove(door);

        if (blocksToMove.getBlocks() != 0)
        {
            if (!isRotateDirectionValid(door))
            {
                plugin.getMyLogger().logMessage("Updating openDirection of sliding door " + door.getName() + " to " + blocksToMove.getRotateDirection() +
                                                ". If this is undesired, change it via the GUI.", true, false);
                plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), blocksToMove.getRotateDirection());
            }
            // Change door availability so it cannot be opened again (just temporarily, don't worry!).
            plugin.getCommander().setDoorBusy(door.getDoorUID());
            plugin.getCommander().addBlockMover(new SlidingMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove.getBlocks(),
                                                                 blocksToMove.getRotateDirection(), plugin.getConfigLoader().sdMultiplier()));
        }
        return DoorOpenResult.SUCCESS;
    }

    private int getBlocksInDir(Door door, RotateDirection slideDir)
    {
        int xMin, xMax, zMin, zMax, yMin, yMax, xLen, zLen, moveBlocks = 0, step;
        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();
        xLen = Math.abs(xMax - xMin) + 1;
        zLen = Math.abs(zMax - zMin) + 1;

        xLen = door.getOpenDir() == RotateDirection.NONE || door.getBlocksToMove() < 1 ? xLen : door.getBlocksToMove();
        zLen = door.getOpenDir() == RotateDirection.NONE || door.getBlocksToMove() < 1 ? zLen : door.getBlocksToMove();

        int xAxis, yAxis, zAxis;
        step = slideDir == RotateDirection.NORTH || slideDir == RotateDirection.WEST ? -1 : 1;

        int startX, startY, startZ, endX, endY, endZ;
        startY = yMin;
        endY   = yMax;
        if (slideDir == RotateDirection.NORTH)
        {
            startZ = zMin - 1;
            endZ   = zMin - zLen - 1;
            startX = xMin;
            endX   = xMax;
        }
        else if (slideDir == RotateDirection.SOUTH)
        {
            startZ = zMax + 1;
            endZ   = zMax + zLen + 1;
            startX = xMin;
            endX   = xMax;
        }
        else if (slideDir == RotateDirection.WEST)
        {
            startZ = zMin;
            endZ   = zMax;
            startX = xMin - 1;
            endX   = xMin - xLen - 1;
        }
        else if (slideDir == RotateDirection.EAST)
        {
            startZ = zMin;
            endZ   = zMax;
            startX = xMax + 1;
            endX   = xMax + xLen + 1;
        }
        else
            return 0;

        World world = door.getWorld();
        if (slideDir == RotateDirection.NORTH || slideDir == RotateDirection.SOUTH)
            for (zAxis = startZ; zAxis != endZ; zAxis += step)
            {
                for (xAxis = startX; xAxis != endX + 1; ++xAxis)
                    for (yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis).getType()))
                            return moveBlocks;
                moveBlocks += step;
            }
        else
            for (xAxis = startX; xAxis != endX; xAxis += step)
            {
                for (zAxis = startZ; zAxis != endZ + 1; ++zAxis)
                    for (yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis).getType()))
                            return moveBlocks;
                moveBlocks += step;
            }
        return moveBlocks;
    }

    private MovementSpecification getBlocksToMove(Door door)
    {
        int blocksNorth = 0, blocksEast = 0, blocksSouth = 0, blocksWest = 0;

        if (door.getOpenDir().equals(RotateDirection.NONE))
        {
            blocksNorth = getBlocksInDir(door, RotateDirection.NORTH);
            blocksSouth = getBlocksInDir(door, RotateDirection.SOUTH);
            blocksEast  = getBlocksInDir(door, RotateDirection.EAST );
            blocksWest  = getBlocksInDir(door, RotateDirection.WEST );
        }
        else if (door.getOpenDir().equals(RotateDirection.NORTH) && !door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.SOUTH) &&  door.isOpen())
            blocksNorth = getBlocksInDir(door, RotateDirection.NORTH);
        else if (door.getOpenDir().equals(RotateDirection.NORTH) &&  door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.SOUTH) && !door.isOpen())
            blocksSouth = getBlocksInDir(door, RotateDirection.SOUTH);
        else if (door.getOpenDir().equals(RotateDirection.EAST ) && !door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.WEST ) &&  door.isOpen())
            blocksEast  = getBlocksInDir(door, RotateDirection.EAST);
        else if (door.getOpenDir().equals(RotateDirection.EAST ) &&  door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.WEST ) && !door.isOpen())
            blocksWest  = getBlocksInDir(door, RotateDirection.WEST);
        else
            return new MovementSpecification(0, RotateDirection.NONE);


        int maxVal = Math.max(Math.abs(blocksNorth), Math.max(blocksEast, Math.max(blocksSouth, Math.abs(blocksWest))));

        if (Math.abs(blocksNorth) == maxVal)
        {
            return new MovementSpecification(blocksNorth, RotateDirection.NORTH);
        }
        if (blocksEast == maxVal)
        {
            return new MovementSpecification(blocksEast, RotateDirection.EAST);
        }
        if (blocksSouth == maxVal)
        {
            return new MovementSpecification(blocksSouth, RotateDirection.SOUTH);
        }
        if (Math.abs(blocksWest) == maxVal)
        {
            return new MovementSpecification(blocksWest, RotateDirection.WEST);
        }
        return new MovementSpecification(0, RotateDirection.NONE);
    }

    private static final class MovementSpecification
    {
        private final int blocks;
        private final RotateDirection rotateDirection;

        MovementSpecification(int blocks, RotateDirection rotateDirection)
        {
            this.blocks = blocks;
            this.rotateDirection = rotateDirection;
        }

        public int getBlocks()
        {
            return blocks;
        }

        public RotateDirection getRotateDirection()
        {
            return rotateDirection;
        }
    }
}




