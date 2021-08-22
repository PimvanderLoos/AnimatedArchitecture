package nl.pim16aap2.bigdoors.api.restartable;

/**
 * Basic implementation of {@link IRestartable}.
 *
 * @author Pim
 */
public abstract class Restartable implements IRestartable
{
    /**
     * Registers a {@link Restartable} with the given holder.
     *
     * @param holder
     *     The {@link IRestartableHolder} to register this {@link Restartable} with.
     */
    protected Restartable(IRestartableHolder holder)
    {
        holder.registerRestartable(this);
    }
}
