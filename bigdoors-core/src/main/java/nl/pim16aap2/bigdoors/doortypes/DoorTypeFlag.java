package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Flag;
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

public final class DoorTypeFlag extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(2);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "northSouth"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "flagDirection"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeFlag instance = new DoorTypeFlag();

    private DoorTypeFlag()
    {
        super(Constants.PLUGINNAME, "Flag", TYPE_VERSION, PARAMETERS);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeFlag get()
    {
        return instance;
    }

    @Override
    public boolean isValidOpenDirection(@NotNull RotateDirection rotateDirection)
    {
        throw new NotImplementedException();
    }

    @Override
    @NotNull
    public List<RotateDirection> getValidOpenDirections()
    {
        throw new NotImplementedException();
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        @Nullable final PBlockFace flagDirection = PBlockFace.valueOf((int) typeData[1]);
        if (flagDirection == null)
            return Optional.empty();

        final boolean onNorthSouthAxis = ((int) typeData[0]) == 1;
        return Optional.of(new Flag(doorData,
                                    onNorthSouthAxis,
                                    flagDirection));
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Flag))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Flag from type: " + door.getDoorType().toString());

        final @NotNull Flag flag = (Flag) door;
        return new Object[]{flag.isNorthSouthAligned() ? 1 : 0,
                            PBlockFace.getValue(flag.getFlagDirection())};
    }
}
