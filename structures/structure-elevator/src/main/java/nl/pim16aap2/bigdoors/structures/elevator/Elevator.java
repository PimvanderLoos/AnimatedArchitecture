package nl.pim16aap2.bigdoors.structures.elevator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.annotations.Deserialization;
import nl.pim16aap2.bigdoors.core.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.structures.portcullis.Portcullis;

/**
 * Represents an Elevator structure type.
 *
 * @author Pim
 * @see Portcullis
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Elevator extends Portcullis
{
    @Deserialization
    public Elevator(BaseHolder base, @PersistentVariable("blocksToMove") int blocksToMove)
    {
        super(base, StructureTypeElevator.get(), blocksToMove);
    }
}