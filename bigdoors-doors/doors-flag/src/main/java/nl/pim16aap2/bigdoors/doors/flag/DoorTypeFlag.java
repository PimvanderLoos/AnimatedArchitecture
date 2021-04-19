package nl.pim16aap2.bigdoors.doors.flag;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class DoorTypeFlag extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final @NonNull DoorTypeFlag INSTANCE = new DoorTypeFlag();

    private DoorTypeFlag()
    {
        super(Constants.PLUGIN_NAME, "Flag", TYPE_VERSION, Collections.emptyList());
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NonNull DoorTypeFlag get()
    {
        return INSTANCE;
    }

    @Override
    public @NonNull Class<? extends AbstractDoorBase> getDoorClass()
    {
        return Flag.class;
    }

    @Override
    public @NonNull Creator getCreator(final @NonNull IPPlayer player)
    {
        return new CreatorFlag(player);
    }

    @Override
    public @NonNull Creator getCreator(final @NonNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorFlag(player, name);
    }
}
