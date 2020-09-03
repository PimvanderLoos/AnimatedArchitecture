package nl.pim16aap2.bigdoors.doors.elevator;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Pair;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeElevator extends DoorType
{
    private static final int TYPE_VERSION = 1;
    @NotNull
    private static final List<Parameter> PARAMETERS;

    static
    {
        final @NotNull List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "blocksToMove"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    private static final @NotNull List<Pair<String, Pair<Integer, Integer>>> dependencies =
        Collections.singletonList(new Pair<>("elevator", new Pair<>(1, 1)));

    @NotNull
    private static final DoorTypeElevator INSTANCE = new DoorTypeElevator();

    private DoorTypeElevator()
    {
        super(Constants.PLUGINNAME, "Elevator", TYPE_VERSION, PARAMETERS,
              Arrays.asList(RotateDirection.UP, RotateDirection.DOWN));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NotNull DoorTypeElevator get()
    {
        return INSTANCE;
    }

    @Override
    public List<Pair<String, Pair<Integer, Integer>>> getDependencies()
    {
        return dependencies;
    }

    @Override
    protected @NotNull Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
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

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorElevator(player);
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorElevator(player, name);
    }

    @Override
    protected @NotNull Object[] generateTypeData(final @NotNull AbstractDoorBase door)
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
