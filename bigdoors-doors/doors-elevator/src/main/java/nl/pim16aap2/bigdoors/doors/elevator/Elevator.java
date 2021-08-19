package nl.pim16aap2.bigdoors.doors.elevator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.doortypes.DoorType;

/**
 * Represents an Elevator doorType.
 *
 * @author Pim
 * @see Portcullis
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Elevator extends Portcullis
{
    private static final DoorType DOOR_TYPE = DoorTypeElevator.get();

    public Elevator(final DoorBase doorData, final int blocksToMove, final int autoCloseTime,
                    final int autoOpenTime)
    {
        super(doorData, blocksToMove, autoCloseTime, autoOpenTime);
    }

    public Elevator(final DoorBase doorBase, final int blocksToMove)
    {
        super(doorBase, blocksToMove, -1, -1);
    }

    private Elevator(final DoorBase doorBase)
    {
        this(doorBase, -1); // Add tmp/default values
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
