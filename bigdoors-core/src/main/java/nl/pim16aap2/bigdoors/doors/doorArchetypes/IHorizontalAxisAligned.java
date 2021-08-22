package nl.pim16aap2.bigdoors.doors.doorArchetypes;

import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;

/**
 * Represents all {@link DoorType}s that are aligned on the North/South or East/West axis. e.g. a sliding door.
 * <p>
 * Only doors with a depth of 1 block can be extended.
 *
 * @author Pim
 * @see DoorBase
 */
public interface IHorizontalAxisAligned
{
    /**
     * Checks if the {@link DoorBase} is aligned with the z-axis (North/South).
     */
    boolean isNorthSouthAligned();
}
