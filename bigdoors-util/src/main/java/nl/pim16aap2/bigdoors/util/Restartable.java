package nl.pim16aap2.bigdoors.util;

/**
 * Represents an object with special behavior on plugin restart.
 *
 * @author Pim
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
