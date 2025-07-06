package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSection;
import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSectionFlag;
import nl.pim16aap2.animatedarchitecture.core.config.StructuresSection;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Spigot-specific implementation of the StructuresSection.
 * <p>
 * This class extends the StructuresSection to provide additional comments and configurations specific to the Spigot
 * platform.
 */
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class StructuresSectionSpigot extends StructuresSection<StructuresSectionSpigot.Result>
{
    private final Lazy<StructureTypeManager> structureTypeManager;

    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    protected String getSectionComment()
    {
        return super.getSectionComment() + """
            
            GUI Material:
              The materials to use in the GUI when looking at the overview of all structures.
            """;
    }

    @Override
    protected IStructureSubSection createSubSection(StructureType structureType)
    {
        if (IStructureSubSectionFlag.STRUCTURE_TYPE_KEY.equals(structureType.getFullKey()))
            return new StructureSubSectionFlagSpigot(structureType);

        return new StructureSubSectionSpigot(structureType);
    }

    @Override
    protected Collection<StructureType> getRegisteredStructureTypes()
    {
        return structureTypeManager.get().getRegisteredStructureTypes();
    }

    @Override
    protected Result getResult(ConfigurationNode sectionNode)
        throws SerializationException
    {
        return new Result();
    }

    public record Result(

    ) implements IConfigSectionResult {}
}
