package nl.pim16aap2.animatedarchitecture.core.api.restartable;


import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents an object that can issue a restart or shutdown to {@link IRestartable} objects.
 */
@Flogger
public final class RestartableHolder implements IDebuggable
{
    private final Set<IRestartable> restartables = new LinkedHashSet<>();
    private int shutdownCount = 0;
    private int initCount = 0;

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
     * Restarts all {@link #restartables}.
     * <p>
     * This is the same as manually calling {@link #shutDown()} and then {@link #initialize()}.
     */
    public void restart()
    {
        shutDown();
        initialize();
    }

    /**
     * Calls {@link IRestartable#initialize()} for all registered {@link IRestartable}s.
     */
    public void initialize()
    {
        this.initCount += 1;
        restartables.forEach(restartable -> runForRestartable("initialize", IRestartable::initialize, restartable));
    }

    /**
     * Calls {@link IRestartable#shutDown()} for all registered {@link IRestartable}s.
     * <p>
     * The {@link #restartables} are shut down in reverse order to ensure that dependents are processed before their
     * dependencies are.
     */
    public void shutDown()
    {
        this.shutdownCount += 1;
        final IRestartable[] arr = restartables.toArray(new IRestartable[0]);
        for (int idx = arr.length - 1; idx >= 0; --idx)
            runForRestartable("shut down", IRestartable::shutDown, arr[idx]);
    }

    private static void runForRestartable(String actionName, Consumer<IRestartable> action, IRestartable restartable)
    {
        try
        {
            action.accept(restartable);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to %s restartable of type %s:\n%s",
                actionName,
                restartable.getClass().getName(),
                restartable
            );
        }
    }

    @Override
    public String getDebugInformation()
    {
        final var sb = new StringBuilder()
            .append("Number of Registered Restartables: ").append(restartables.size()).append('\n')
            .append("ShutDownCount: ").append(shutdownCount).append('\n')
            .append("InitCount: ").append(initCount).append('\n')
            .append("Registered Restartables:\n");

        restartables.forEach(restartable -> sb.append("  - ").append(restartable.getClass().getName()).append('\n'));
        return sb.toString();
    }
}
