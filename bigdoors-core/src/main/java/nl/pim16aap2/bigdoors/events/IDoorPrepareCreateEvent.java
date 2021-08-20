package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.doors.AbstractDoor;

/**
 * Represents the event where a door will be created.
 *
 * @author Pim
 */
public interface IDoorPrepareCreateEvent extends IDoorEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the {@link AbstractDoor} that was created.
     * <p>
     * Note that this is NOT the final {@link AbstractDoor} that will exist after creation; it is merely a preview!
     *
     * @return The {@link AbstractDoor} that will be created.
     */
    @Override
    AbstractDoor getDoor();
}
