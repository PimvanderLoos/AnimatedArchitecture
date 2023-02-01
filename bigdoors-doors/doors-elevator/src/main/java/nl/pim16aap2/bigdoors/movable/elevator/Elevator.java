package nl.pim16aap2.bigdoors.movable.elevator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;

/**
 * Represents an Elevator movable type.
 *
 * @author Pim
 * @see Portcullis
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Elevator extends Portcullis
{
    @DeserializationConstructor
    public Elevator(AbstractMovable.MovableBaseHolder base, @PersistentVariable("blocksToMove") int blocksToMove)
    {
        super(base, MovableTypeElevator.get(), blocksToMove);
    }
}
