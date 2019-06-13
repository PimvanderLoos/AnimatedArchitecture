package nl.pim16aap2.bigdoors.moveblocks;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

public class PortcullisOpener extends Opener
{
    public PortcullisOpener(BigDoors plugin)
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

        Location newMin = door.getMinimum().clone();
        Location newMax = door.getMaximum().clone();
        int blocksToMove = getBlocksToMove(door, newMin, newMax);

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), newMin, newMax) != null)
            return abort(door, DoorOpenResult.NOPERMISSION);

        if (blocksToMove != 0)
            plugin.addBlockMover(new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove,
                                                   plugin.getConfigLoader().getMultiplier(DoorType.PORTCULLIS)));
        else
            return abort(door, DoorOpenResult.NODIRECTION);
        return DoorOpenResult.SUCCESS;
    }

    private int getBlocksInDir(DoorBase door, RotateDirection upDown)
    {
        int xMin, xMax, zMin, zMax, yMin, yMax, yLen, blocksUp = 0, delta;
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
        delta = upDown == RotateDirection.DOWN ? -1 : 1;
        yAxis = upDown == RotateDirection.DOWN ? yMin - 1 : yMax + 1;
        yGoal = upDown == RotateDirection.DOWN ? yMin - distanceToCheck - 1 : yMax + distanceToCheck + 1;

        while (yAxis != yGoal)
        {
            for (xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (zAxis = zMin; zAxis <= zMax; ++zAxis)
                    if (!Util.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                        return blocksUp;
            yAxis += delta;
            blocksUp += delta;
        }
        return blocksUp;
    }

    private int getBlocksToMove(DoorBase door, Location newMin, Location newMax)
    {
        int blocksUp = getBlocksInDir(door, RotateDirection.UP);
        int blocksDown = getBlocksInDir(door, RotateDirection.DOWN);
        int blocksToMove = blocksUp > -1 * blocksDown ? blocksUp : blocksDown;

        newMin.add(0, blocksToMove, 0);
        newMax.add(0, blocksToMove, 0);

        return blocksToMove;
    }
}
