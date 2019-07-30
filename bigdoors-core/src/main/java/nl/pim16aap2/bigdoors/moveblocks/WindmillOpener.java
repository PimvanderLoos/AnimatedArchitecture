package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an object that toggles windmills.
 */
public class WindmillOpener extends Opener
{
    public WindmillOpener(final @NotNull BigDoors plugin)
    {
        super(plugin);
    }

    /**
     * Gets the direction a {@link DoorBase} will move.
     *
     * @param door The {@link DoorBase}.
     * @return The direction a {@link DoorBase} will move.
     */
    @NotNull
    private RotateDirection getOpenDirection(final @NotNull DoorBase door)
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
              .addBlockMover(new WindmillMover(plugin, door.getWorld(), door, time,
                                               plugin.getConfigLoader().getMultiplier(DoorType.WINDMILL),
                                               getOpenDirection(door), playerUUID));
        return DoorToggleResult.SUCCESS;
    }
}
