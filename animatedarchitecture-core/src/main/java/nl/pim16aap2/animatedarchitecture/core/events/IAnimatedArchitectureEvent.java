package nl.pim16aap2.animatedarchitecture.core.events;


/**
 * Represents a AnimatedArchitecture event.
 *
 * @author Pim
 */
public interface IAnimatedArchitectureEvent
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
