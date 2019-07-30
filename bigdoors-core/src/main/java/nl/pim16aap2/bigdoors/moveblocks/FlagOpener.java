package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlagOpener extends Opener
{
    public FlagOpener(final @NotNull BigDoors plugin)
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
            return abort(door, DoorToggleResult.ERROR);

        plugin.getDatabaseManager()
              .addBlockMover(
                  new FlagMover(plugin, door.getWorld(), 60, door,
                                plugin.getConfigLoader().getMultiplier(DoorType.FLAG), playerUUID));

        return DoorToggleResult.SUCCESS;
    }
}
