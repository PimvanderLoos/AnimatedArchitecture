package nl.pim16aap2.bigdoors.structures.structurearchetypes;

import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;

/**
 * Represents all {@link StructureType}s that are aligned on the North/South or East/West axis. e.g. a sliding
 * structure.
 * <p>
 * Only structures with a depth of 1 block can be extended.
 *
 * @author Pim
 * @see AbstractStructure
 */
public interface IHorizontalAxisAligned
{
    /**
     * Checks if the {@link AbstractStructure} is aligned with the z-axis (North/South).
     */
    boolean isNorthSouthAligned();
}
