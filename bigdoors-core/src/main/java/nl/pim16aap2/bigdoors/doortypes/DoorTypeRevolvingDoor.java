package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.RevolvingDoor;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorRevolvingDoor;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeRevolvingDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(1);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "qCircles"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeRevolvingDoor instance = new DoorTypeRevolvingDoor();

    private DoorTypeRevolvingDoor()
    {
        super(Constants.PLUGINNAME, "RevolvingDoor", TYPE_VERSION, PARAMETERS, Arrays
            .asList(RotateDirection.CLOCKWISE, RotateDirection.COUNTERCLOCKWISE));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public @NotNull
    static DoorTypeRevolvingDoor get()
    {
        return instance;
    }

    @Override
    @NotNull
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        final int qCircles = (int) typeData[0];
        return Optional.of(new RevolvingDoor(doorData,
                                             qCircles));
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorRevolvingDoor(player);
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorRevolvingDoor(player, name);
    }

    @Override
    @NotNull
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof RevolvingDoor))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an RevolvingDoor from type: " +
                    door.getDoorType().toString());

        final @NotNull RevolvingDoor revolvingDoor = (RevolvingDoor) door;
        return new Object[]{revolvingDoor.getQuarterCircles()};
    }
}
