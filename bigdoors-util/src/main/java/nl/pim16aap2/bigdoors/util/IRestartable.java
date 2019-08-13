package nl.pim16aap2.bigdoors.util;

/**
 * Represents an object with special behavior on plugin restart.
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
     * Handles a shutdown..
     */
    void shutdown();
}
