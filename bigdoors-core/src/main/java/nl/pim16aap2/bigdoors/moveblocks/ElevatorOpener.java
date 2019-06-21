package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.DoorOpenResult;
import nl.pim16aap2.bigdoors.spigotutil.Util;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.World;

public class ElevatorOpener extends Opener
{
    MyBlockFace ddirection;

    public ElevatorOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(DoorBase door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return abort(door, isOpenable);
        super.setBusy(door);

        if (super.isTooBig(door))
            instantOpen = true;

        int blocksToMove = getBlocksToMove(door);
        if (blocksToMove != 0)
            plugin.addBlockMover(new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove,
                                                   plugin.getConfigLoader().getMultiplier(DoorType.ELEVATOR)));
        else
            return abort(door, DoorOpenResult.NODIRECTION);
        return DoorOpenResult.SUCCESS;
    }

    private int getBlocksInDir(DoorBase door, RotateDirection upDown)
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
                    if (!Util.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                        return blocksMoved;
            yAxis += step;
            blocksMoved += step;
        }
        return blocksMoved;
    }

    private int getBlocksToMove(DoorBase door)
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
