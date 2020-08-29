package nl.pim16aap2.bigdoors.events;

import org.jetbrains.annotations.NotNull;

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
    @NotNull String getEventName();

    /**
     * Checks if the event is fired asynchronous.
     *
     * @return True if the event is fired asynchronous.
     */
    boolean isAsynchronous();
}
