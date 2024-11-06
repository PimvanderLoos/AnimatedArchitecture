package nl.pim16aap2.animatedarchitecture.structures.windmill;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructureTypeWindmill extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeWindmill INSTANCE = new StructureTypeWindmill();

    private StructureTypeWindmill()
    {
        super(
            NamespacedKey.of("Windmill"),
            TYPE_VERSION,
            List.of(
                MovementDirection.NORTH,
                MovementDirection.EAST,
                MovementDirection.SOUTH,
                MovementDirection.WEST),
            List.of(
                Property.ANIMATION_SPEED_MULTIPLIER,
                Property.ROTATION_POINT
            ),
            "structure.type.windmill"
        );
    }

    /**
     * Getter for the type for this structure.
     *
     * @return The instance of this type.
     */
    public static StructureTypeWindmill get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
    {
        return Windmill.class;
    }

    @Override
    public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorWindMill(context, player, name);
    }
}
