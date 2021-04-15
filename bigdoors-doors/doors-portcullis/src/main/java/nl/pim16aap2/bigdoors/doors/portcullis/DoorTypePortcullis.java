package nl.pim16aap2.bigdoors.doors.portcullis;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypePortcullis extends DoorType
{
    private static final int TYPE_VERSION = 1;

    @NonNull
    private static final DoorTypePortcullis INSTANCE = new DoorTypePortcullis();

    private DoorTypePortcullis()
    {
        super(Constants.PLUGINNAME, "Portcullis", TYPE_VERSION,
              Arrays.asList(RotateDirection.UP, RotateDirection.DOWN));
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NonNull DoorTypePortcullis get()
    {
        return INSTANCE;
    }

    @Override
    public @NonNull Class<? extends AbstractDoorBase> getDoorClass()
    {
        return Portcullis.class;
    }

    @Override
    public @NonNull Creator getCreator(final @NonNull IPPlayer player)
    {
        return new CreatorPortcullis(player);
    }

    @Override
    public @NonNull Creator getCreator(final @NonNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorPortcullis(player, name);
    }
}
