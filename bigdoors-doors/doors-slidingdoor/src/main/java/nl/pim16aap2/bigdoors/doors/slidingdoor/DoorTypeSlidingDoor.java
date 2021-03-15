package nl.pim16aap2.bigdoors.doors.slidingdoor;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeSlidingDoor extends DoorType
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

    @NotNull
    private static final DoorTypeSlidingDoor INSTANCE = new DoorTypeSlidingDoor();

    private DoorTypeSlidingDoor()
    {
        super(Constants.PLUGINNAME, "SlidingDoor", TYPE_VERSION, PARAMETERS, Arrays
            .asList(RotateDirection.NORTH, RotateDirection.EAST,
                    RotateDirection.SOUTH, RotateDirection.WEST));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NotNull DoorTypeSlidingDoor get()
    {
        return INSTANCE;
    }

    @Override
    public @NonNull Class<? extends AbstractDoorBase> getDoorClass()
    {
        return SlidingDoor.class;
    }

    @Override
    protected @NotNull Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                              final @NotNull Object... typeData)
    {
        final int blocksToMove = (int) typeData[0];
        final int autoCloseTimer = (int) typeData[1];
        final int autoOpenTimer = (int) typeData[2];

        return Optional.of(new SlidingDoor(doorData,
                                           blocksToMove,
                                           autoCloseTimer,
                                           autoOpenTimer));
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorSlidingDoor(player);
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorSlidingDoor(player, name);
    }

    @Override
    protected @NotNull Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof SlidingDoor))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an SlidingDoor from type: " + door.getDoorType().toString());

        final @NotNull SlidingDoor slidingDoor = (SlidingDoor) door;
        return new Object[]{slidingDoor.getBlocksToMove(),
                            slidingDoor.getAutoCloseTime(),
                            slidingDoor.getAutoOpenTime()};
    }
}
