package nl.pim16aap2.bigdoors.api.restartable;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a basic implementation of {@link IRestartableHolder}.
 *
 * @author Pim
 */
public abstract class RestartableHolder implements IRestartableHolder, IRestartable
{
    protected final @NonNull Set<@NonNull IRestartable> restartables = new HashSet<>();

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NotNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NotNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }

    @Override
    public void restart()
    {
        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void shutdown()
    {
        restartables.forEach(IRestartable::shutdown);
    }

}
