package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Windmill;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeWindmill extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(2);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "northSouth"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "qCircles"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeWindmill instance = new DoorTypeWindmill();

    private DoorTypeWindmill()
    {
        super(Constants.PLUGINNAME, "Windmill", TYPE_VERSION, PARAMETERS);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeWindmill get()
    {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        final boolean onNorthSouthAxis = ((int) typeData[0]) == 1;
        final int quarterCircles = (int) typeData[1];
        return Optional.of(new Windmill(doorData,
                                        onNorthSouthAxis,
                                        quarterCircles));
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Windmill))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Windmill from type: " + door.getDoorType().toString());

        final @NotNull Windmill windmill = (Windmill) door;
        return new Object[]{windmill.getOnNorthSouthAxis() ? 1 : 0,
                            windmill.getQuarterCircles()};
    }
}
