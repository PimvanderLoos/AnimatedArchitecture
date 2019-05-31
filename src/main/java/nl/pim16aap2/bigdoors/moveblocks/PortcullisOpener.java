package nl.pim16aap2.bigdoors.moveblocks;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;
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
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return abort(door, isOpenable);
        super.setBusy(door);

        if (super.isTooBig(door))
            instantOpen = true;

        int blocksToMove = getBlocksToMove(door);

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getNewMin(), door.getNewMax()) != null)
            return abort(door, DoorOpenResult.NOPERMISSION);

        if (blocksToMove != 0)
            plugin.addBlockMover(new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove,
                                                   plugin.getConfigLoader().getMultiplier(DoorType.PORTCULLIS)));
        else
            return abort(door, DoorOpenResult.NODIRECTION);
        return DoorOpenResult.SUCCESS;
    }

    private int getBlocksInDir(Door door, RotateDirection upDown)
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
                    if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis)))
                        return blocksUp;
            yAxis += delta;
            blocksUp += delta;
        }
        return blocksUp;
    }

    private int getBlocksToMove(Door door)
    {
        int blocksUp = getBlocksInDir(door, RotateDirection.UP);
        int blocksDown = getBlocksInDir(door, RotateDirection.DOWN);
        int blocksToMove = blocksUp > -1 * blocksDown ? blocksUp : blocksDown;
        door.setNewMin(new Location(door.getWorld(), door.getMinimum().getBlockX(),
                                    door.getMinimum().getBlockY() + blocksToMove, door.getMinimum().getBlockZ()));
        door.setNewMax(new Location(door.getWorld(), door.getMaximum().getBlockX(),
                                    door.getMaximum().getBlockY() + blocksToMove, door.getMaximum().getBlockZ()));
        return blocksToMove;
    }
}
