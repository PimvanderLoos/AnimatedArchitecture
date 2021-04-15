package nl.pim16aap2.bigdoors.doors.windmill;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypeWindmill extends DoorType
{
    private static final int TYPE_VERSION = 1;

    @NonNull
    private static final DoorTypeWindmill INSTANCE = new DoorTypeWindmill();

    private DoorTypeWindmill()
    {
        super(Constants.PLUGINNAME, "Windmill", TYPE_VERSION,
              Arrays.asList(RotateDirection.NORTH, RotateDirection.EAST,
                            RotateDirection.SOUTH, RotateDirection.WEST));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NonNull DoorTypeWindmill get()
    {
        return INSTANCE;
    }

    @Override
    public @NonNull Class<? extends AbstractDoorBase> getDoorClass()
    {
        return Windmill.class;
    }

    @Override
    public @NonNull Creator getCreator(final @NonNull IPPlayer player)
    {
        return new CreatorWindMill(player);
    }

    @Override
    public @NonNull Creator getCreator(final @NonNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorWindMill(player, name);
    }
}
