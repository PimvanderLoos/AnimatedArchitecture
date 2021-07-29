package nl.pim16aap2.bigdoors.doors.garagedoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypeGarageDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final @NotNull DoorTypeGarageDoor INSTANCE = new DoorTypeGarageDoor();

    private DoorTypeGarageDoor()
    {
        super(Constants.PLUGIN_NAME, "GarageDoor", TYPE_VERSION,
              Arrays.asList(RotateDirection.NORTH, RotateDirection.EAST,
                            RotateDirection.SOUTH, RotateDirection.WEST));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NotNull DoorTypeGarageDoor get()
    {
        return INSTANCE;
    }

    @Override
    public @NotNull Class<? extends AbstractDoor> getDoorClass()
    {
        return GarageDoor.class;
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorGarageDoor(player);
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorGarageDoor(player, name);
    }
}
