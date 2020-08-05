package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Clock;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeClock extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(2);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "northSouth"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "hourArmSide"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeClock instance = new DoorTypeClock();

    private DoorTypeClock()
    {
        super(Constants.PLUGINNAME, "Clock", TYPE_VERSION, PARAMETERS);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeClock get()
    {
        return instance;
    }

    @Override
    public boolean isValidOpenDirection(@NotNull RotateDirection rotateDirection)
    {
        throw new NotImplementedException();
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        @Nullable final PBlockFace hourArmSide = PBlockFace.valueOf((int) typeData[1]);
        if (hourArmSide == null)
            return Optional.empty();

        final boolean onNorthSouthAxis = ((int) typeData[0]) == 1;
        return Optional.of(new Clock(doorData,
                                     onNorthSouthAxis,
                                     hourArmSide));
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Clock))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Clock from type: " + door.getDoorType().toString());

        final @NotNull Clock clock = (Clock) door;
        return new Object[]{clock.isNorthSouthAligned() ? 1 : 0,
                            PBlockFace.getValue(clock.getHourArmSide())};
    }
}
