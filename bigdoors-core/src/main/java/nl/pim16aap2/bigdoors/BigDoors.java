package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;

import javax.inject.Singleton;

/**
 * Represents the core class of BigDoors.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
@Singleton
public final class BigDoors extends RestartableHolder
{
    /**
     * Handles a restart.
     */
    public void restart()
    {
        restartables.forEach(IRestartable::restart);
    }

    /**
     * Handles a shutdown.
     */
    public void shutdown()
    {
        restartables.forEach(IRestartable::shutdown);
    }
}
