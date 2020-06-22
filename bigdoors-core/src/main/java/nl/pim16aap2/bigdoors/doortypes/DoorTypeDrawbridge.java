package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Drawbridge;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeDrawbridge extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(4);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "currentDirection"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "modeUpDown"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "northSouthAligned"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeDrawbridge instance = new DoorTypeDrawbridge();

    private DoorTypeDrawbridge()
    {
        super(Constants.PLUGINNAME, "DrawBridge", TYPE_VERSION, PARAMETERS);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeDrawbridge get()
    {
        return instance;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        @Nullable final PBlockFace currentDirection = PBlockFace.valueOf((int) typeData[2]);
        if (currentDirection == null)
            return Optional.empty();

        final int autoCloseTimer = (int) typeData[0];
        final int autoOpenTimer = (int) typeData[1];

        final boolean modeUP = ((int) typeData[3]) == 1;
        final boolean northSouthAligned = ((int) typeData[4]) == 1;
        return Optional.of(new Drawbridge(doorData,
                                          autoCloseTimer,
                                          autoOpenTimer,
                                          currentDirection,
                                          modeUP,
                                          northSouthAligned));
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Drawbridge))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Drawbridge from type: " + door.getDoorType().toString());

        final @NotNull Drawbridge drawbridge = (Drawbridge) door;
        return new Object[]{drawbridge.getAutoCloseTime(),
                            drawbridge.getAutoOpenTime(),
                            PBlockFace.getValue(drawbridge.getCurrentDirection()),
                            drawbridge.isModeUp() ? 1 : 0,
                            drawbridge.isNorthSouthAligned() ? 1 : 0};
    }
}
