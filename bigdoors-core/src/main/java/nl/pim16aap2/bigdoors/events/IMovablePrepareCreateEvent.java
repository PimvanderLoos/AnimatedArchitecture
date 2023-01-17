package nl.pim16aap2.bigdoors.events;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

/**
 * Represents the event where a movable will be created.
 *
 * @author Pim
 */
public interface IMovablePrepareCreateEvent extends IMovableEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the {@link AbstractMovable} that was created.
     * <p>
     * Note that this is NOT the final {@link AbstractMovable} that will exist after creation; it is merely a preview!
     *
     * @return The {@link AbstractMovable} that will be created.
     */
    @Override
    AbstractMovable getMovable();
}
