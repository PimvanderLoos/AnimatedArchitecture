package nl.pim16aap2.bigdoors.util;

import org.jetbrains.annotations.NotNull;

/**
 * {@inheritDoc}
 */
public abstract class Restartable implements IRestartable
{
    /**
     * Registers a {@link Restartable} with the given holder.
     *
     * @param holder The {@link IRestartableHolder} to register this {@link Restartable} with.
     */
    protected Restartable(final @NotNull IRestartableHolder holder)
    {
        holder.registerRestartable(this);
    }
}
