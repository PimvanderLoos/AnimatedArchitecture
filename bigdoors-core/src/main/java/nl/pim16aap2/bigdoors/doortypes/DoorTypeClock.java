package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Clock;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorClock;
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
        super(Constants.PLUGINNAME, "Clock", TYPE_VERSION, PARAMETERS,
              Arrays.asList(RotateDirection.NORTH, RotateDirection.EAST,
                            RotateDirection.SOUTH, RotateDirection.WEST));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public @NotNull
    static DoorTypeClock get()
    {
        return instance;
    }

    @Override
    @NotNull
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        final @Nullable PBlockFace hourArmSide = PBlockFace.valueOf((int) typeData[1]);
        if (hourArmSide == null)
            return Optional.empty();

        final boolean onNorthSouthAxis = ((int) typeData[0]) == 1;
        return Optional.of(new Clock(doorData,
                                     onNorthSouthAxis,
                                     hourArmSide));
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorClock(player);
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorClock(player, name);
    }

    @Override
    @NotNull
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        // TODO: Handle with logger.
        if (!(door instanceof Clock))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Clock from type: " + door.getDoorType().toString());

        final @NotNull Clock clock = (Clock) door;
        return new Object[]{clock.isNorthSouthAligned() ? 1 : 0,
                            PBlockFace.getValue(clock.getHourArmSide())};
    }
}
