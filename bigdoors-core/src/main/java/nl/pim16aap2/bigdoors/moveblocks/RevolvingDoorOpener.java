package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class RevolvingDoorOpener extends Opener
{
    public RevolvingDoorOpener(BigDoors plugin)
    {
        super(plugin);
    }

    private RotateDirection getOpenDirection(DoorBase door)
    {
        // Default to Clockwise rotation when nothing else has bee specified.
        if (door.getOpenDir().equals(RotateDirection.NONE))
        {
            RotateDirection newDirection = RotateDirection.CLOCKWISE;
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

        if (super.isTooBig(door))
            return abort(door, DoorOpenResult.ERROR);

        plugin.addBlockMover(new RevolvingDoorMover(plugin, door.getWorld(), door, time,
                                                    plugin.getConfigLoader().getMultiplier(DoorType.REVOLVINGDOOR),
                                                    getOpenDirection(door)));
        return DoorOpenResult.SUCCESS;
    }
}
