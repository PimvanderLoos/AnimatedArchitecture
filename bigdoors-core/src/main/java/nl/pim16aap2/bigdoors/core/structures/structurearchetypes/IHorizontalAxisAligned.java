package nl.pim16aap2.bigdoors.core.structures.structurearchetypes;

import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureType;

/**
 * Represents all {@link StructureType}s that are aligned on the North/South or East/West axis. e.g. a sliding door.
 */
public interface IHorizontalAxisAligned
{
    /**
     * Checks if the {@link AbstractStructure} is aligned with the z-axis (North/South).
     */
    boolean isNorthSouthAligned();
}
