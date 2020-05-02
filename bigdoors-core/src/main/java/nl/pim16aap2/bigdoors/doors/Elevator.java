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
        return Optional.of(new Elevator(doorData, (int) args[0]));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof Elevator))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an Elevator from type: " + door.getDoorType().toString());

        final @NotNull Elevator elevator = (Elevator) door;
        return new Object[]{elevator.getBlocksToMove()};
    }

    public Elevator(final @NotNull DoorData doorData, final int blocksToMove)
    {
        super(doorData, blocksToMove);
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
        return super.equals(o) && getClass() == o.getClass();
    }
}
