package nl.pim16aap2.bigdoors.api.restartable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a basic implementation of {@link IRestartableHolder}.
 *
 * @author Pim
 */
public class RestartableHolder implements IRestartableHolder
{
    protected final Set<IRestartable> restartables = new LinkedHashSet<>();

    @Override
    public void registerRestartable(IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(IRestartable restartable)
    {
        restartables.remove(restartable);
    }
}
