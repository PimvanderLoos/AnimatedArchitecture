package nl.pim16aap2.animatedarchitecture.core.events;

/**
 * Represents a cancellable AnimatedArchitecture event.
 *
 * @see IAnimatedArchitectureEvent
 */
public interface ICancellableAnimatedArchitectureEvent extends IAnimatedArchitectureEvent
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
     * @param status
     *     True to cancel the event.
     */
    void setCancelled(boolean status);
}
