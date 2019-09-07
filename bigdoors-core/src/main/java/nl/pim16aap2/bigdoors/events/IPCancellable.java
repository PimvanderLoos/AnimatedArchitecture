package nl.pim16aap2.bigdoors.events;

/**
 * Represents a cancellable BigDoors event.
 *
 * @author Pim
 * @see PEvent
 */
public interface IPCancellable
{
    /**
     * Checks if this event has been cancelled or not.
     *
     * @return True if it has been cancelled.
     */
    public boolean isCancelled();

    /**
     * Sets the cancelled status of this event.
     *
     * @param status True to cancel the event.
     */
    public void setCancelled(boolean status);
}
