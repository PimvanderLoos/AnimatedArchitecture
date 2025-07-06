package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSection;
import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSectionFlag;
import nl.pim16aap2.animatedarchitecture.core.config.StructuresSection;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

/**
 * Spigot-specific implementation of the StructuresSection.
 * <p>
 * This class extends the StructuresSection to provide additional comments and configurations specific to the Spigot
 * platform.
 */
public class StructuresSectionSpigot extends StructuresSection
{
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
}
