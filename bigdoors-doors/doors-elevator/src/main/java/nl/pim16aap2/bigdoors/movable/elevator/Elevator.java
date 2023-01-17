package nl.pim16aap2.bigdoors.movable.elevator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.portcullis.Portcullis;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;

/**
 * Represents an Elevator doorType.
 *
 * @author Pim
 * @see Portcullis
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Elevator extends Portcullis
{
    private static final MovableType MOVABLE_TYPE = MovableTypeElevator.get();

    public Elevator(MovableBase doorData, int blocksToMove, int autoCloseTime, int autoOpenTime)
    {
        super(doorData, blocksToMove, autoCloseTime, autoOpenTime);
    }

    public Elevator(MovableBase doorBase, int blocksToMove)
    {
        super(doorBase, blocksToMove, -1, -1);
    }

    @SuppressWarnings("unused")
    private Elevator(MovableBase doorBase)
    {
        this(doorBase, -1); // Add tmp/default values
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }
}
