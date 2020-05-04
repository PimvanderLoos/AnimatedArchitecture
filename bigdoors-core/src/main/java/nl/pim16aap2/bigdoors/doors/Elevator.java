package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeElevator;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents an Elevator doorType.
 *
 * @author Pim
 * @see Portcullis
 */
public class Elevator extends Portcullis
{
    private static final DoorType DOOR_TYPE = DoorTypeElevator.get();

    @NotNull
    public static Optional<AbstractDoorBase> constructor(final @NotNull DoorData doorData,
                                                         final @NotNull Object... args)
        throws Exception
    {
        final int blocksToMove = (int) args[0];
        final int autoCloseTimer = (int) args[1];
        final int autoOpenTimer = (int) args[2];

        return Optional.of(new Elevator(doorData,
                                        blocksToMove,
                                        autoCloseTimer,
                                        autoOpenTimer));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof Elevator))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an Elevator from type: " + door.getDoorType().toString());

        final @NotNull Elevator elevator = (Elevator) door;
        return new Object[]{elevator.blocksToMove,
                            elevator.autoCloseTime,
                            elevator.autoOpenTime};
    }

    public Elevator(final @NotNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                    final int autoOpenTime)
    {
        super(doorData, blocksToMove, autoCloseTime, autoOpenTime);
    }

    @Deprecated
    protected Elevator(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                       final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    @Deprecated
    protected Elevator(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.ELEVATOR);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    /**
     * {@inheritDoc}
     */
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
