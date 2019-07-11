package nl.pim16aap2.bigdoors.util;

/**
 * {@inheritDoc}
 */
public abstract class Restartable implements IRestartable
{
    protected final IRestartableHolder holder;

    protected Restartable(final IRestartableHolder holder)
    {
        this.holder = holder;
        holder.registerRestartable(this);
    }
}
