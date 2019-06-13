package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class WindmillOpener extends Opener
{
    public WindmillOpener(BigDoors plugin)
    {
        super(plugin);
    }

    private RotateDirection getOpenDirection(DoorBase door)
    {
        // First check if the user has already specified a direction for the windmill to
        // turn. If not, default to North if positioned along the Z axis, or East with
        // positioned along the x axis.
        if (door.getOpenDir().equals(RotateDirection.NONE))
        {
            // Check if the door is positioned along the North/South axis. I.e.: zDepth > 1.
            boolean NS = Math.abs(door.getMinimum().getBlockZ() - door.getMaximum().getBlockZ()) != 0;
            RotateDirection newDirection = NS ? RotateDirection.NORTH : RotateDirection.EAST;
            plugin.getDatabaseManager().updateDoorOpenDirection(door.getDoorUID(), newDirection);
            door.setOpenDir(newDirection);
        }
        return door.getOpenDir();
    }

    @Override
    public DoorOpenResult openDoor(DoorBase door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return abort(door, isOpenable);
        super.setBusy(door);

        if (super.isTooBig(door))
            return abort(door, DoorOpenResult.ERROR);

        plugin.addBlockMover(new WindmillMover(plugin, door.getWorld(), door,
                                               plugin.getConfigLoader().getMultiplier(DoorType.WINDMILL),
                                               getOpenDirection(door)));
        return DoorOpenResult.SUCCESS;
    }
}
