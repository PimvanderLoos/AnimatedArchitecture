package nl.pim16aap2.bigdoors.doors.bigdoor;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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

    @NotNull
    private static final DoorTypeBigDoor INSTANCE = new DoorTypeBigDoor();

    private DoorTypeBigDoor()
    {
        super(Constants.PLUGINNAME, "BigDoor", TYPE_VERSION,
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
    public @NonNull Class<? extends AbstractDoorBase> getDoorClass()
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
