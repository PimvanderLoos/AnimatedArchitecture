package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Drawbridge;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorDrawbridge;
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

public final class DoorTypeDrawbridge extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "modeUpDown"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeDrawbridge instance = new DoorTypeDrawbridge();

    private DoorTypeDrawbridge()
    {
        super(Constants.PLUGINNAME, "DrawBridge", TYPE_VERSION, PARAMETERS,
              Arrays.asList(RotateDirection.NORTH, RotateDirection.EAST,
                            RotateDirection.SOUTH, RotateDirection.WEST));
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorDrawbridge(player);
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorDrawbridge(player, name);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public @NotNull
    static DoorTypeDrawbridge get()
    {
        return instance;
    }

    @Override
    @NotNull
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        final @Nullable PBlockFace currentDirection = PBlockFace.valueOf((int) typeData[2]);
        if (currentDirection == null)
            return Optional.empty();

        final int autoCloseTimer = (int) typeData[0];
        final int autoOpenTimer = (int) typeData[1];

        final boolean modeUP = ((int) typeData[2]) == 1;
        return Optional.of(new Drawbridge(doorData,
                                          autoCloseTimer,
                                          autoOpenTimer,
                                          modeUP));
    }

    @Override
    @NotNull
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Drawbridge))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Drawbridge from type: " + door.getDoorType().toString());

        final @NotNull Drawbridge drawbridge = (Drawbridge) door;
        return new Object[]{drawbridge.getAutoCloseTime(),
                            drawbridge.getAutoOpenTime(),
                            drawbridge.isModeUp() ? 1 : 0};
    }
}
