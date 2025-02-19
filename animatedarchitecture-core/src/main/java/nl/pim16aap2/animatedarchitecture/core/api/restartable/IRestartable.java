package nl.pim16aap2.animatedarchitecture.core.api.restartable;

/**
 * Represents an object what can be restarted and shut down.
 */
public interface IRestartable
{
    /**
     * Handles the initialization.
     */
    default void initialize()
    {
    }

    /**
     * Handles a shutdown.
     */
    default void shutDown()
    {
    }
}
