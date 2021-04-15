package nl.pim16aap2.bigdoors.events;


import lombok.NonNull;

/**
 * Represents a BigDoors event.
 *
 * @author Pim
 */
public interface IPEvent
{
    /**
     * Gets the name of this event.
     *
     * @return The name of this event.
     */
    @NonNull String getEventName();

    /**
     * Checks if the event is fired asynchronous.
     *
     * @return True if the event is fired asynchronous.
     */
    boolean isAsynchronous();
}
