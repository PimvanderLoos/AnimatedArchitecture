package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.GarageDoor;
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

public final class DoorTypeGarageDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(4);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "northSouth"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "currentDirection"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeGarageDoor instance = new DoorTypeGarageDoor();

    private DoorTypeGarageDoor()
    {
        super(Constants.PLUGINNAME, "GarageDoor", TYPE_VERSION, PARAMETERS);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeGarageDoor get()
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
        @Nullable final PBlockFace currentDirection = PBlockFace.valueOf((int) typeData[3]);
        if (currentDirection == null)
            return Optional.empty();

        final int autoCloseTimer = (int) typeData[0];
        final int autoOpenTimer = (int) typeData[1];
        final boolean onNorthSouthAxis = ((int) typeData[2]) == 1;

        return Optional.of(new GarageDoor(doorData,
                                          autoCloseTimer,
                                          autoOpenTimer,
                                          onNorthSouthAxis,
                                          currentDirection));
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof GarageDoor))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a GarageDoor from type: " + door.getDoorType().toString());

        final @NotNull GarageDoor garageDoor = (GarageDoor) door;
        return new Object[]{garageDoor.getAutoCloseTime(),
                            garageDoor.getAutoOpenTime(),
                            garageDoor.isNorthSouthAligned() ? 1 : 0,
                            PBlockFace.getValue(garageDoor.getCurrentDirection())};
    }
}
