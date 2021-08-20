package nl.pim16aap2.bigdoors.doors.flag;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class DoorTypeFlag extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final DoorTypeFlag INSTANCE = new DoorTypeFlag();

    private DoorTypeFlag()
    {
        super(Constants.PLUGIN_NAME, "Flag", TYPE_VERSION, Collections.emptyList(), "door.type.flag");
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static DoorTypeFlag get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractDoor> getDoorClass()
    {
        return Flag.class;
    }

    @Override
    public Creator getCreator(IPPlayer player)
    {
        return new CreatorFlag(player);
    }

    @Override
    public Creator getCreator(IPPlayer player, @Nullable String name)
    {
        return new CreatorFlag(player, name);
    }
}
