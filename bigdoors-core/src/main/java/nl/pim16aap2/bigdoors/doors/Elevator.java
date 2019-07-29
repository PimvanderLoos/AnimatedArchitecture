package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an Elevator doorType.
 *
 * @author Pim
 * @see Portcullis
 */
public class Elevator extends Portcullis
{
    Elevator(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Elevator(final @NotNull PLogger pLogger, final long doorUID)
    {
        super(pLogger, doorUID, DoorType.ELEVATOR);
    }
}
