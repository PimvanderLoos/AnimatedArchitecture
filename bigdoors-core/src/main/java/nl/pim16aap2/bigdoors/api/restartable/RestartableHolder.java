package nl.pim16aap2.bigdoors.api.restartable;


import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents an object that can issue a restart or shutdown to {@link IRestartable} objects.
 *
 * @author Pim
 */
public final class RestartableHolder
{
    private final Set<IRestartable> restartables = new LinkedHashSet<>();

    /**
     * Register a {@link IRestartable} object with this object, so this object can restart the provided object.
     *
     * @param restartable
     *     A {@link IRestartable} object that can be restarted by this object.
     */
    public void registerRestartable(IRestartable restartable)
    {
        restartables.add(restartable);
    }

    /**
     * Checks if a {@link IRestartable} has been registered with this object.
     *
     * @param restartable
     *     The {@link IRestartable} to check.
     * @return True if the {@link IRestartable} has been registered with this object.
     */
    @SuppressWarnings("unused")
    public boolean isRestartableRegistered(IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    /**
     * Deregisters an {@link IRestartable} if it is currently registered.
     *
     * @param restartable
     *     The {@link IRestartable} to deregister.
     */
    public void deregisterRestartable(IRestartable restartable)
    {
        restartables.remove(restartable);
    }

    /**
     * Calls {@link IRestartable#restart()} for all registered {@link IRestartable}s.
     */
    public void restart()
    {
        restartables.forEach(IRestartable::restart);
    }

    /**
     * Calls {@link IRestartable#shutdown()} ()} for all registered {@link IRestartable}s.
     */
    public void shutdown()
    {
        restartables.forEach(IRestartable::shutdown);
    }
}
