package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;

public class FlagOpener extends Opener
{
    public FlagOpener(BigDoors plugin)
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

        if (super.isTooBig(door))
            return abort(door, DoorOpenResult.ERROR);

        plugin.addBlockMover(new FlagMover(plugin, door.getWorld(), 60, door,
                                           plugin.getConfigLoader().getMultiplier(DoorType.FLAG)));

        return DoorOpenResult.SUCCESS;
    }
}
