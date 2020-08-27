package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Portcullis;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorPortcullis;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypePortcullis extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "blocksToMove"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypePortcullis instance = new DoorTypePortcullis();

    private DoorTypePortcullis()
    {
        super(Constants.PLUGINNAME, "Portcullis", TYPE_VERSION, PARAMETERS,
              Arrays.asList(RotateDirection.UP, RotateDirection.DOWN));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public @NotNull
    static DoorTypePortcullis get()
    {
        return instance;
    }

    @Override
    @NotNull
    protected Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                     final @NotNull Object... typeData)
    {
        final int blocksToMove = (int) typeData[0];
        final int autoCloseTimer = (int) typeData[1];
        final int autoOpenTimer = (int) typeData[2];

        return Optional.of(new Portcullis(doorData,
                                          blocksToMove,
                                          autoCloseTimer,
                                          autoOpenTimer));
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorPortcullis(player);
    }

    @Override
    @NotNull
    public Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorPortcullis(player, name);
    }

    @Override
    @NotNull
    protected Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Portcullis))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an Portcullis from type: " + door.getDoorType().toString());

        final @NotNull Portcullis portcullis = (Portcullis) door;
        return new Object[]{portcullis.getBlocksToMove(),
                            portcullis.getAutoCloseTime(),
                            portcullis.getAutoOpenTime()};
    }
}
