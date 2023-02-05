package nl.pim16aap2.bigdoors.core.events;


/**
 * Represents a BigDoors event.
 *
 * @author Pim
 */
public interface IBigDoorsEvent
{
    /**
     * Gets the name of this event.
     *
     * @return The name of this event.
     */
    String getEventName();

    /**
     * Checks if the event is fired asynchronous.
     *
     * @return True if the event is fired asynchronous.
     */
    boolean isAsynchronous();
}
