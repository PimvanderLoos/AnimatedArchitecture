package nl.pim16aap2.bigdoors.api.restartable;

/**
 * Represents an object what can be restarted and shut down.
 *
 * @author Pim
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
    void shutDown();
}
