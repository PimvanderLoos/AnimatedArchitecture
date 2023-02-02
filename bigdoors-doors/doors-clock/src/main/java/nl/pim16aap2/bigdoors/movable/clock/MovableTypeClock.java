package nl.pim16aap2.bigdoors.movable.clock;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class MovableTypeClock extends MovableType
{
    private static final int TYPE_VERSION = 1;

    private static final MovableTypeClock INSTANCE = new MovableTypeClock();

    private MovableTypeClock()
    {
        super(Constants.PLUGIN_NAME, "Clock", TYPE_VERSION,
              Arrays.asList(MovementDirection.NORTH, MovementDirection.EAST,
                            MovementDirection.SOUTH, MovementDirection.WEST), "movable.type.clock");
    }

    /**
     * @return The instance of this type.
     */
    public static MovableTypeClock get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractMovable> getMovableClass()
    {
        return Clock.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorClock(context, player, name);
    }
}
