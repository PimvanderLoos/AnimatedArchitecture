package nl.pim16aap2.bigdoors.api.restartable;


import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Represents an object that can issue a restart or shutdown to {@link IRestartable} objects.
 *
 * @author Pim
 */
@Flogger
public final class RestartableHolder
{
    private final Set<IRestartable> restartables = new LinkedHashSet<>();

    /**
     * Register a {@link IRestartable} object with this object, so this object can restart the provided object.
     *
     * @param restartable
     *     A {@link IRestartable} object that can be restarted by this object.
     * @throws NullPointerException
     *     If the provided restartable is null.
     */
    public void registerRestartable(IRestartable restartable)
    {
        restartables.add(Objects.requireNonNull(restartable, "Cannot register null restartable!"));
    }

    /**
     * Checks if a {@link IRestartable} has been registered with this object.
     *
     * @param restartable
     *     The {@link IRestartable} to check.
     * @return True if the {@link IRestartable} has been registered with this object.
     */
    @SuppressWarnings("unused")
    public boolean isRestartableRegistered(@Nullable IRestartable restartable)
    {
        if (restartable == null)
            return false;
        return restartables.contains(restartable);
    }

    /**
     * Deregisters an {@link IRestartable} if it is currently registered.
     *
     * @param restartable
     *     The {@link IRestartable} to deregister.
     * @throws NullPointerException
     *     If the provided restartable is null.
     */
    public void deregisterRestartable(IRestartable restartable)
    {
        restartables.remove(Objects.requireNonNull(restartable, "Cannot deregister null restartable!"));
    }

    /**
     * Calls {@link IRestartable#restart()} for all registered {@link IRestartable}s.
     */
    public void restart()
    {
        restartables.forEach(restartable -> runForRestartable("restart", IRestartable::restart, restartable));
    }

    /**
     * Calls {@link IRestartable#shutdown()} for all registered {@link IRestartable}s.
     * <p>
     * The {@link #restartables} are shut down in reverse order to ensure that dependents are processed before their
     * dependencies are.
     */
    public void shutdown()
    {
        final IRestartable[] arr = restartables.toArray(new IRestartable[0]);
        for (int idx = arr.length - 1; idx >= 0; --idx)
            runForRestartable("shut down", IRestartable::shutdown, arr[idx]);
    }

    private static void runForRestartable(String actionName, Consumer<IRestartable> action, IRestartable restartable)
    {
        try
        {
            action.accept(restartable);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to %s restartable of type %s:\n%s",
                                                  actionName, restartable.getClass().getName(), restartable);
        }
    }
}
