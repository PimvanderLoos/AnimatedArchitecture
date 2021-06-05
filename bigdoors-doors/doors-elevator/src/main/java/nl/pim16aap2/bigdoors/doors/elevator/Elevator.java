package nl.pim16aap2.bigdoors.doors.elevator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;

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
    private static final @NotNull DoorType DOOR_TYPE = DoorTypeElevator.get();

    public Elevator(final @NotNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                    final int autoOpenTime)
    {
        super(doorData, blocksToMove, autoCloseTime, autoOpenTime);
    }

    public Elevator(final @NotNull DoorData doorData, final int blocksToMove)
    {
        super(doorData, blocksToMove, -1, -1);
    }

    private Elevator(final @NotNull DoorData doorData)
    {
        this(doorData, -1); // Add tmp/default values
    }

    @Override
    public @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
