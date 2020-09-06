package nl.pim16aap2.bigdoors.doors.flag;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeFlag extends DoorType
{
    private static final int TYPE_VERSION = 1;
    @NotNull
    private static final List<Parameter> PARAMETERS;

    static
    {
        final @NotNull List<Parameter> parameterTMP = new ArrayList<>(1);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "northSouth"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeFlag INSTANCE = new DoorTypeFlag();

    private DoorTypeFlag()
    {
        super(Constants.PLUGINNAME, "Flag", TYPE_VERSION, PARAMETERS, Collections.emptyList());
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NotNull DoorTypeFlag get()
    {
        return INSTANCE;
    }

    @Override
    protected @NotNull Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                              final @NotNull Object... typeData)
    {
        final boolean onNorthSouthAxis = ((int) typeData[0]) == 1;
        return Optional.of(new Flag(doorData,
                                    onNorthSouthAxis));
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorFlag(player);
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorFlag(player, name);
    }

    @Override
    protected @NotNull Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Flag))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Flag from type: " + door.getDoorType().toString());

        final @NotNull Flag flag = (Flag) door;
        return new Object[]{flag.isNorthSouthAligned() ? 1 : 0};
    }
}
