package nl.pim16aap2.bigdoors.movable.flag;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class MovableTypeFlag extends MovableType
{
    private static final int TYPE_VERSION = 1;

    private static final MovableTypeFlag INSTANCE = new MovableTypeFlag();

    private MovableTypeFlag()
    {
        super(Constants.PLUGIN_NAME, "Flag", TYPE_VERSION, Collections.emptyList(), "door.type.flag");
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static MovableTypeFlag get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractMovable> getMovableClass()
    {
        return Flag.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorFlag(context, player, name);
    }
}
