package nl.pim16aap2.bigdoors.movable.elevator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;

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
    private static final MovableType MOVABLE_TYPE = MovableTypeElevator.get();

    @DeserializationConstructor
    public Elevator(AbstractMovable.MovableBaseHolder base, @PersistentVariable("blocksToMove") int blocksToMove)
    {
        super(base, blocksToMove);
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }
}
