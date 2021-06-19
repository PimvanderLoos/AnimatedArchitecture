package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the event where a door will be created.
 *
 * @author Pim
 */
public interface IDoorPrepareCreateEvent extends IDoorEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the {@link AbstractDoorBase} that was created.
     * <p>
     * Note that this is NOT the final {@link AbstractDoorBase} that will exist after creation; it is merely a preview!
     *
     * @return The {@link AbstractDoorBase} that will be created.
     */
    @Override
    @NotNull AbstractDoorBase getDoor();
}
