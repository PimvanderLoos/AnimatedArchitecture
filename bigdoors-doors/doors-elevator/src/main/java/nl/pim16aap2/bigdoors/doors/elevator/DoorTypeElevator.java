package nl.pim16aap2.bigdoors.doors.elevator;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypeElevator extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final @NotNull DoorTypeElevator INSTANCE = new DoorTypeElevator();

    private DoorTypeElevator()
    {
        super(Constants.PLUGIN_NAME, "Elevator", TYPE_VERSION,
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
    public @NotNull Class<? extends AbstractDoorBase> getDoorClass()
    {
        return Elevator.class;
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
}
