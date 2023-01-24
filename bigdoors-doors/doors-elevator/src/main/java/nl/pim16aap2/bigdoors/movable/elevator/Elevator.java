package nl.pim16aap2.bigdoors.movable.elevator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.portcullis.Portcullis;
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

    public Elevator(AbstractMovable.MovableBaseHolder base, int blocksToMove)
    {
        super(base, blocksToMove);
    }

    @SuppressWarnings("unused")
    private Elevator(AbstractMovable.MovableBaseHolder base)
    {
        this(base, -1); // Add tmp/default values
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }
}
