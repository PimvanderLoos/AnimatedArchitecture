package nl.pim16aap2.animatedarchitecture.structures.flag;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class StructureTypeFlag extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeFlag INSTANCE = new StructureTypeFlag();

    private StructureTypeFlag()
    {
        super(Constants.PLUGIN_NAME, "Flag", TYPE_VERSION, Collections.emptyList(), "structure.type.flag");
    }

    /**
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
    public Creator getCreator(Creator.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorFlag(context, player, name);
    }
}
