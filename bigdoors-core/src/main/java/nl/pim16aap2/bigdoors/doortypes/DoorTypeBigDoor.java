package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeBigDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "currentDirection"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final List<RotateDirection> VALID_ROTATE_DIRECTIONS =
        Collections.unmodifiableList(Arrays.asList(RotateDirection.CLOCKWISE, RotateDirection.COUNTERCLOCKWISE));


    @NotNull
    private static final DoorTypeBigDoor instance = new DoorTypeBigDoor();

    private DoorTypeBigDoor()
    {
        super(Constants.PLUGINNAME, "BigDoor", TYPE_VERSION, PARAMETERS);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeBigDoor get()
    {
        return instance;
    }

    @Override
    public boolean isValidOpenDirection(@NotNull RotateDirection rotateDirection)
    {
        return DoorTypeBigDoor.VALID_ROTATE_DIRECTIONS.contains(rotateDirection);
    }

    @Override
    public @NotNull List<RotateDirection> getValidOpenDirections()
    {
        return VALID_ROTATE_DIRECTIONS;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
        throws Exception
    {
        final @Nullable PBlockFace currentDirection = PBlockFace.valueOf((int) typeData[2]);
        if (currentDirection == null)
            return Optional.empty();

        final int autoCloseTimer = (int) typeData[0];
        final int autoOpenTimer = (int) typeData[1];

        return Optional.of(new BigDoor(doorData,
                                       autoCloseTimer,
                                       autoOpenTimer,
                                       currentDirection));
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
        throws Exception
    {
        if (!(door instanceof BigDoor))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a BigDoor from type: " + door.getDoorType().toString());

        final @NotNull BigDoor bigDoor = (BigDoor) door;
        return new Object[]{bigDoor.getAutoCloseTime(),
                            bigDoor.getAutoOpenTime(),
                            PBlockFace.getValue(bigDoor.getCurrentDirection())};
    }
}
