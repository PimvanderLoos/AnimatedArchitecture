package nl.pim16aap2.animatedarchitecture.structures.clock;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructureTypeClock extends StructureType
{
    private static final int TYPE_VERSION = 10;

    private static final StructureTypeClock INSTANCE = new StructureTypeClock();

    private StructureTypeClock()
    {
        super(
            NamespacedKey.of("Clock"),
            TYPE_VERSION,
            List.of(
                MovementDirection.NORTH,
                MovementDirection.EAST,
                MovementDirection.SOUTH,
                MovementDirection.WEST),
            List.of(
                Property.ROTATION_POINT
            ),
            "structure.type.clock"
        );
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
    public IStructureComponent newComponent()
    {
        return new Clock();
    }

    @Override
    public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorClock(context, player, name);
    }
}
