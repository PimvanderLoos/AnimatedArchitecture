package nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

/**
 * Represents all {@link StructureType}s that are aligned on the North/South or East/West axis. e.g. a sliding door.
 */
public interface IHorizontalAxisAligned
{
    /**
     * Describes if the {@link AbstractStructure} is animated along the North/South (-/+ Z) axis <b>(= TRUE)</b> or
     * along the East/West (+/- X) axis <b>(= FALSE)</b>.
     *
     * @return True if this structure is animated along the North/South axis.
     */
    boolean isNorthSouthAnimated();
}
