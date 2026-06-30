package nl.pim16aap2.animatedarchitecture.core.api.restartable;

/**
 * Represents an object what can be restarted and shut down.
 */
public interface IRestartable
{
    /**
     * Handles the initialization.
     *
     * @throws Exception
     *     May throw an exception depending on the implementation.
     */
    default void initialize()
        throws Exception
    {
    }

    /**
     * Handles a shutdown.
     *
     * @throws Exception
     *     May throw an exception depending on the implementation.
     */
    default void shutDown()
        throws Exception
    {
    }
}
