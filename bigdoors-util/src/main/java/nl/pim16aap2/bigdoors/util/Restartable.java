package nl.pim16aap2.bigdoors.util;

/**
 * {@inheritDoc}
 */
public abstract class Restartable implements IRestartable
{
    protected final RestartableHolder holder;

    protected Restartable(final RestartableHolder holder)
    {
        this.holder = holder;
        holder.registerRestartable(this);
    }
}
