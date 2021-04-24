package nl.pim16aap2.bigdoors.api.restartable;

import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a basic implementation of {@link IRestartableHolder}.
 *
 * @author Pim
 */
public class RestartableHolder implements IRestartableHolder
{
    protected final @NonNull Set<@NonNull IRestartable> restartables = new HashSet<>();

    @Override
    public void registerRestartable(final @NonNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NonNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NonNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }
}
