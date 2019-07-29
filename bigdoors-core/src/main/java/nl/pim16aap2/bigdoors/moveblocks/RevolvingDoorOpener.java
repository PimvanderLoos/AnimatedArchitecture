package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RevolvingDoorOpener extends Opener
{
    public RevolvingDoorOpener(final @NotNull BigDoors plugin)
    {
        super(plugin);
    }

    /**
     * Gets the opening direction of a {@link DoorBase}.
     *
     * @param door The {@link DoorBase}.
     * @return The direction this {@link DoorBase} will rotate.
     */
    private RotateDirection getOpenDirection(final @NotNull DoorBase door)
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

        plugin.addBlockMover(new RevolvingDoorMover(plugin, door.getWorld(), door, time,
                                                    plugin.getConfigLoader().getMultiplier(DoorType.REVOLVINGDOOR),
                                                    getOpenDirection(door), playerUUID));
        return DoorToggleResult.SUCCESS;
    }
}
