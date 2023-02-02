package nl.pim16aap2.bigdoors.structures.clock;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class StructureTypeClock extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeClock INSTANCE = new StructureTypeClock();

    private StructureTypeClock()
    {
        super(Constants.PLUGIN_NAME, "Clock", TYPE_VERSION,
              Arrays.asList(MovementDirection.NORTH, MovementDirection.EAST,
                            MovementDirection.SOUTH, MovementDirection.WEST), "structure.type.clock");
    }

    /**
     * @return The instance of this type.
     */
    public static StructureTypeClock get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
    {
        return Clock.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorClock(context, player, name);
    }
}
