package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import lombok.AllArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSection;
import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSectionFlag;
import nl.pim16aap2.animatedarchitecture.core.config.StructuresSection;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.Collection;

/**
 * Spigot-specific implementation of the StructuresSection.
 * <p>
 * This class extends the StructuresSection to provide additional comments and configurations specific to the Spigot
 * platform.
 */
@AllArgsConstructor
public class StructuresSectionSpigot extends StructuresSection
{
    private final Lazy<StructureTypeManager> structureTypeManager;

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
}
