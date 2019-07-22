package nl.pim16aap2.bigdoors.util;

/**
 * Represents an object with special behavior on plugin restart.
 *
 * @author Pim
 */
public interface IRestartable
{
    /**
     * Handle a restart.
     */
    void restart();

    /**
     * Handle a shutdown..
     */
    void shutdown();
}
