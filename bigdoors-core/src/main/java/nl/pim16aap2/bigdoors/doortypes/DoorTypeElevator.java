package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Elevator;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeElevator extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "blocksToMove"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeElevator instance = new DoorTypeElevator();

    private DoorTypeElevator()
    {
        super(Constants.PLUGINNAME, "Elevator", TYPE_VERSION, PARAMETERS);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeElevator get()
    {
        return instance;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        final int blocksToMove = (int) typeData[0];
        final int autoCloseTimer = (int) typeData[1];
        final int autoOpenTimer = (int) typeData[2];

        return Optional.of(new Elevator(doorData,
                                        blocksToMove,
                                        autoCloseTimer,
                                        autoOpenTimer));
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Elevator))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an Elevator from type: " + door.getDoorType().toString());

        final @NotNull Elevator elevator = (Elevator) door;
        return new Object[]{elevator.getBlocksToMove(),
                            elevator.getAutoCloseTime(),
                            elevator.getAutoOpenTime()};
    }
}
