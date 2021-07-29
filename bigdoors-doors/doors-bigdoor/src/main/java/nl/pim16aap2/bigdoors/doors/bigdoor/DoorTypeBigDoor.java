package nl.pim16aap2.bigdoors.doors.bigdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypeBigDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final @NotNull DoorTypeBigDoor INSTANCE = new DoorTypeBigDoor();

    private DoorTypeBigDoor()
    {
        super(Constants.PLUGIN_NAME, "BigDoor", TYPE_VERSION,
              Arrays.asList(RotateDirection.CLOCKWISE, RotateDirection.COUNTERCLOCKWISE));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NotNull DoorTypeBigDoor get()
    {
        return INSTANCE;
    }

    @Override
    public @NotNull Class<? extends AbstractDoor> getDoorClass()
    {
        return BigDoor.class;
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorBigDoor(player);
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorBigDoor(player, name);
    }
}
