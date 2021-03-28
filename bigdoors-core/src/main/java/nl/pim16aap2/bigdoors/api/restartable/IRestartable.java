package nl.pim16aap2.bigdoors.api.restartable;

/**
 * Represents an object what can be restarted and shut down.
 *
 * @author Pim
 */
public interface IRestartable
{
    /**
     * Handles a restart.
     */
    void restart();

    /**
     * Handles a shutdown.
     */
    void shutdown();
}
