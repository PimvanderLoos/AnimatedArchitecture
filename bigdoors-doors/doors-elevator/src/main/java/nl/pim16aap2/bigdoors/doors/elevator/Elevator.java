package nl.pim16aap2.bigdoors.doors.elevator;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.doors.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an Elevator doorType.
 *
 * @author Pim
 * @see Portcullis
 */
public class Elevator extends Portcullis
{
    @NonNull
    private static final DoorType DOOR_TYPE = DoorTypeElevator.get();

    public Elevator(final @NonNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                    final int autoOpenTime)
    {
        super(doorData, blocksToMove, autoCloseTime, autoOpenTime);
    }

    public Elevator(final @NonNull DoorData doorData, final int blocksToMove)
    {
        super(doorData, blocksToMove, -1, -1);
    }

    private Elevator(final @NonNull DoorData doorData)
    {
        this(doorData, -1); // Add tmp/default values
    }

    @Override
    public @NonNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public boolean equals(final @Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NonNull Elevator other = (Elevator) o;
        return blocksToMove == other.blocksToMove &&
            autoOpenTime == other.autoOpenTime &&
            autoCloseTime == other.autoCloseTime;
    }
}
