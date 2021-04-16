package nl.pim16aap2.bigdoors.events;

/**
 * Represents a cancellable BigDoors event.
 *
 * @author Pim
 * @see IBigDoorsEvent
 */
public interface ICancellableBigDoorsEvent extends IBigDoorsEvent
{
    /**
     * Checks if this event has been cancelled or not.
     *
     * @return True if it has been cancelled.
     */
    boolean isCancelled();

    /**
     * Sets the cancelled status of this event.
     *
     * @param status True to cancel the event.
     */
    void setCancelled(boolean status);
}
