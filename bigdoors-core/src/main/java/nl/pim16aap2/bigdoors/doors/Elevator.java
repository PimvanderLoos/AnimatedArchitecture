package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeElevator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an Elevator doorType.
 *
 * @author Pim
 * @see Portcullis
 */
public class Elevator extends Portcullis
{
    private static final DoorType DOOR_TYPE = DoorTypeElevator.get();

    public Elevator(final @NotNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                    final int autoOpenTime)
    {
        super(doorData, blocksToMove, autoCloseTime, autoOpenTime);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public DoorType getDoorType()
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

        final @NotNull Elevator other = (Elevator) o;
        return blocksToMove == other.blocksToMove &&
            autoOpenTime == other.autoOpenTime &&
            autoCloseTime == other.autoCloseTime;
    }
}
