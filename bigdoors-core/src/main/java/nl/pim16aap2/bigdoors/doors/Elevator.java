package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.PLogger;

/**
 * Represents an Elevator doorType.
 *
 * @author Pim
 * @see Portcullis
 */
public class Elevator extends Portcullis
{
    Elevator(PLogger pLogger, long doorUID, DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Elevator(PLogger pLogger, long doorUID)
    {
        super(pLogger, doorUID, DoorType.ELEVATOR);
    }
}
