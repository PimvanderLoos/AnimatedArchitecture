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
    protected Elevator(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                       final @NotNull DoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected Elevator(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, DoorType.ELEVATOR);
    }
}
