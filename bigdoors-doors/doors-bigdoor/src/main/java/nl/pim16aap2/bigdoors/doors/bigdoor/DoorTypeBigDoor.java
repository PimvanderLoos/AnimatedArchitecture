package nl.pim16aap2.bigdoors.doors.bigdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypeBigDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final DoorTypeBigDoor INSTANCE = new DoorTypeBigDoor();

    private DoorTypeBigDoor()
    {
        super(Constants.PLUGIN_NAME, "BigDoor", TYPE_VERSION,
              Arrays.asList(RotateDirection.CLOCKWISE, RotateDirection.COUNTERCLOCKWISE), "door.type.big_door");
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static DoorTypeBigDoor get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractDoor> getDoorClass()
    {
        return BigDoor.class;
    }

    @Override
    public Creator getCreator(IPPlayer player)
    {
        return new CreatorBigDoor(player);
    }

    @Override
    public Creator getCreator(IPPlayer player, @Nullable String name)
    {
        return new CreatorBigDoor(player, name);
    }
}
