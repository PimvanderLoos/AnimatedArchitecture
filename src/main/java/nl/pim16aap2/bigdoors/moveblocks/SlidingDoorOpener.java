package nl.pim16aap2.bigdoors.moveblocks;

import org.bukkit.World;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

public class SlidingDoorOpener extends Opener
{
    private RotateDirection moveDirection;

    public SlidingDoorOpener(final BigDoors plugin)
    {
        super(plugin);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return abort(door, isOpenable);
        super.setBusy(door);

        if (super.isTooBig(door))
            instantOpen = true;

        int blocksToMove = getBlocksToMove(door);

        if (blocksToMove != 0)
            plugin.addBlockMover(new SlidingMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove,
                                                  moveDirection,
                                                  plugin.getConfigLoader().getMultiplier(DoorType.SLIDINGDOOR)));
        else
            return abort(door, DoorOpenResult.NODIRECTION);
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
        endY = yMax;
        if (slideDir == RotateDirection.NORTH)
        {
            startZ = zMin - 1;
            endZ = zMin - zLen - 1;
            startX = xMin;
            endX = xMax;
        }
        else if (slideDir == RotateDirection.SOUTH)
        {
            startZ = zMax + 1;
            endZ = zMax + zLen + 1;
            startX = xMin;
            endX = xMax;
        }
        else if (slideDir == RotateDirection.WEST)
        {
            startZ = zMin;
            endZ = zMax;
            startX = xMin - 1;
            endX = xMin - xLen - 1;
        }
        else if (slideDir == RotateDirection.EAST)
        {
            startZ = zMin;
            endZ = zMax;
            startX = xMax + 1;
            endX = xMax + xLen + 1;
        }
        else
            return 0;

        World world = door.getWorld();
        if (slideDir == RotateDirection.NORTH || slideDir == RotateDirection.SOUTH)
            for (zAxis = startZ; zAxis != endZ; zAxis += step)
            {
                for (xAxis = startX; xAxis != endX + 1; ++xAxis)
                    for (yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return moveBlocks;
                moveBlocks += step;
            }
        else
            for (xAxis = startX; xAxis != endX; xAxis += step)
            {
                for (zAxis = startZ; zAxis != endZ + 1; ++zAxis)
                    for (yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return moveBlocks;
                moveBlocks += step;
            }
        return moveBlocks;
    }

    private int getBlocksToMove(Door door)
    {
        int blocksNorth = 0, blocksEast = 0, blocksSouth = 0, blocksWest = 0;

        if (door.getOpenDir().equals(RotateDirection.NONE))
        {
            blocksNorth = getBlocksInDir(door, RotateDirection.NORTH);
            blocksSouth = getBlocksInDir(door, RotateDirection.SOUTH);
            blocksEast = getBlocksInDir(door, RotateDirection.EAST);
            blocksWest = getBlocksInDir(door, RotateDirection.WEST);
        }
        else if (door.getOpenDir().equals(RotateDirection.NORTH) && !door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.SOUTH) && door.isOpen())
        {
            blocksNorth = getBlocksInDir(door, RotateDirection.NORTH);
        }
        else if (door.getOpenDir().equals(RotateDirection.NORTH) && door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.SOUTH) && !door.isOpen())
        {
            blocksSouth = getBlocksInDir(door, RotateDirection.SOUTH);
        }
        else if (door.getOpenDir().equals(RotateDirection.EAST) && !door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.WEST) && door.isOpen())
        {
            blocksEast = getBlocksInDir(door, RotateDirection.EAST);
        }
        else if (door.getOpenDir().equals(RotateDirection.EAST) && door.isOpen() ||
                 door.getOpenDir().equals(RotateDirection.WEST) && !door.isOpen())
        {
            blocksWest = getBlocksInDir(door, RotateDirection.WEST);
        }
        else
        {
            return 0;
        }

        int maxVal = Math.max(Math.abs(blocksNorth), Math.max(blocksEast, Math.max(blocksSouth, Math.abs(blocksWest))));

        if (Math.abs(blocksNorth) == maxVal)
        {
            moveDirection = RotateDirection.NORTH;
            return blocksNorth;
        }
        if (blocksEast == maxVal)
        {
            moveDirection = RotateDirection.EAST;
            return blocksEast;
        }
        if (blocksSouth == maxVal)
        {
            moveDirection = RotateDirection.SOUTH;
            return blocksSouth;
        }
        if (Math.abs(blocksWest) == maxVal)
        {
            moveDirection = RotateDirection.WEST;
            return blocksWest;
        }
        return -1;
    }
}
