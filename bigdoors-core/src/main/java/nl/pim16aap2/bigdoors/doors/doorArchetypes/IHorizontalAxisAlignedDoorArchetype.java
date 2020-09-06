package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.IDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

/**
 * Represents all {@link DoorType}s that are aligned on the North/South or East/West axis. e.g. a sliding door.
 * <p>
 * Only doors with a depth of 1 block can be extended.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public interface IHorizontalAxisAlignedDoorArchetype extends IDoorBase
{
    /**
     * Checks if the {@link AbstractDoorBase} is aligned with the z-axis (North/South).
     */
    boolean isNorthSouthAligned();

    @Override
    default @NotNull RotateDirection cycleOpenDirection()
    {
        if (isNorthSouthAligned())
            return getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.SOUTH : RotateDirection.NORTH;
    }
}
