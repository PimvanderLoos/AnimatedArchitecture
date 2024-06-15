package nl.pim16aap2.animatedarchitecture.core.api.restartable;

/**
 * Basic implementation of {@link IRestartable}.
 */
public abstract class Restartable implements IRestartable
{
    /**
     * Registers a {@link Restartable} with the given holder.
     *
     * @param holder
     *     The {@link RestartableHolder} to register this {@link Restartable} with.
     */
    protected Restartable(RestartableHolder holder)
    {
        holder.registerRestartable(this);
    }
}
