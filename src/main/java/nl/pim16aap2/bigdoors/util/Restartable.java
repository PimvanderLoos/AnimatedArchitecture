package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.BigDoors;

/**
 * Represents an object with special behavior on plugin restart.
 *
 * @author Pim
 */
public abstract class Restartable
{
    protected final BigDoors plugin;

    protected Restartable(final BigDoors plugin)
    {
        this.plugin = plugin;
        plugin.registerRestartable(this);
    }

    /**
     * Handle a restart.
     */
    public abstract void restart();
}
