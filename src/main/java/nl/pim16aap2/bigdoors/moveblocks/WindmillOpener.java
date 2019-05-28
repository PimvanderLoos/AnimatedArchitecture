package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;

public class WindmillOpener extends Opener
{
    public WindmillOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return isOpenable;
        super.setBusy(door);

        if (super.isTooBig(door))
            return DoorOpenResult.ERROR;

        plugin.addBlockMover(new WindmillMover(plugin, door.getWorld(), 60, door,
                                           plugin.getConfigLoader().getMultiplier(DoorType.FLAG)));

        return DoorOpenResult.SUCCESS;
    }
}
