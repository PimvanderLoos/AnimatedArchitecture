package nl.pim16aap2.animatedarchitecture.structures.clock;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
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
     * Getter for the type for this structure.
     *
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
    public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorClock(context, player, name);
    }
}
