package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SlidingDoorOpener extends Opener
{
    public SlidingDoorOpener(final @NotNull BigDoors plugin)
    {
        super(plugin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorToggleResult toggleDoor(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                                       final double time, boolean instantOpen, final boolean playerToggle)
    {
        DoorToggleResult isOpenable = super.canBeToggled(door, playerToggle);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return abort(door, isOpenable);

        if (super.isTooBig(door))
            instantOpen = true;

        Mutable<Integer> blocksToMove = new Mutable<>(null);
        Mutable<RotateDirection> rotateDirection = new Mutable<>(null);
        getBlocksToMove(door, blocksToMove, rotateDirection);

        if (blocksToMove.isEmpty() || rotateDirection.isEmpty())
            return abort(door, DoorToggleResult.NODIRECTION);

        Location newMin = door.getMinimum().clone();
        Location newMax = door.getMaximum().clone();
        door.getNewLocations(null, rotateDirection.getVal(), newMin, newMax, blocksToMove.getVal(), null);

        // Check if the owner of the door has permission to edit blocks in the new area of the door.
        if (!super.canBreakBlocksBetweenLocs(door, newMin, newMax))
            return abort(door, DoorToggleResult.NOPERMISSION);

        plugin.addBlockMover(new SlidingMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove.getVal(),
                                              rotateDirection.getVal(),
                                              plugin.getConfigLoader().getMultiplier(DoorType.SLIDINGDOOR),
                                              playerUUID));
        return DoorToggleResult.SUCCESS;
    }

    /**
     * Gets the number of blocks a {@link DoorBase} can move in a direction.
     *
     * @param door     The {@link DoorBase}.
     * @param slideDir The direction.
     * @return The number of blocks a {@link DoorBase} can move in a direction.
     */
    private int getBlocksInDir(final @NotNull DoorBase door, final @NotNull RotateDirection slideDir)
    {
        int xMin, xMax, zMin, zMax, yMin, yMax, xLen, zLen, moveBlocks = 0, step;
        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();

        // xLen and zLen describe the length of the door in the x and the z direction respectively.
        // If the rotation direction and the blocksToMove variable are defined, use the blocksToMove variable instead.
        xLen = (door.getOpenDir() == RotateDirection.NONE || (door.getBlocksToMove() < 1)) ?
               Math.abs(xMax - xMin) + 1 : door.getBlocksToMove();
        zLen = (door.getOpenDir() == RotateDirection.NONE || (door.getBlocksToMove() < 1)) ?
               Math.abs(zMax - zMin) + 1 : door.getBlocksToMove();

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
                        if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return moveBlocks;
                moveBlocks += step;
            }
        else
            for (xAxis = startX; xAxis != endX; xAxis += step)
            {
                for (zAxis = startZ; zAxis != endZ + 1; ++zAxis)
                    for (yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return moveBlocks;
                moveBlocks += step;
            }
        return moveBlocks;
    }

    /**
     * Gets the number of blocks a {@link DoorBase} can move in a direction.
     *
     * @param door          The {@link DoorBase}.
     * @param blocksToMove  The number of blocks the {@link DoorBase} can move in a direction. The value is set in the
     *                      method.
     * @param openDirection The direction the {@link DoorBase} can move. The value is set in the method.
     */
    private void getBlocksToMove(final @NotNull DoorBase door, final @NotNull Mutable<Integer> blocksToMove,
                                 final @NotNull Mutable<RotateDirection> openDirection)
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
            return;

        int maxVal = Math.max(Math.abs(blocksNorth), Math.max(blocksEast, Math.max(blocksSouth, Math.abs(blocksWest))));

        if (Math.abs(blocksNorth) == maxVal)
        {
            blocksToMove.setVal(blocksNorth);
            openDirection.setVal(RotateDirection.NORTH);
        }
        else if (blocksEast == maxVal)
        {
            blocksToMove.setVal(blocksEast);
            openDirection.setVal(RotateDirection.EAST);
        }
        else if (blocksSouth == maxVal)
        {
            blocksToMove.setVal(blocksSouth);
            openDirection.setVal(RotateDirection.SOUTH);
        }
        else if (Math.abs(blocksWest) == maxVal)
        {
            blocksToMove.setVal(blocksWest);
            openDirection.setVal(RotateDirection.WEST);
        }
    }
}
