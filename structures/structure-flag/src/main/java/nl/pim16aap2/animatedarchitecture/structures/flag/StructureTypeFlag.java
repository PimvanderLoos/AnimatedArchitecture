package nl.pim16aap2.animatedarchitecture.structures.flag;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructureTypeFlag extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeFlag INSTANCE = new StructureTypeFlag();

    private StructureTypeFlag()
    {
        super(
            NamespacedKey.of("Flag"),
            TYPE_VERSION,
            List.of(),
            List.of(
                Property.ROTATION_POINT
            ),
            "structure.type.flag"
        );
    }

    /**
     * Getter for the type for this structure.
     *
     * @return The instance of this type.
     */
    public static StructureTypeFlag get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
    {
        return Flag.class;
    }

    @Override
    public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorFlag(context, player, name);
    }
}
