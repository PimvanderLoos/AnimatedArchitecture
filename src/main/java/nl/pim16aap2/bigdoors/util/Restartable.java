package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.BigDoors;

public abstract class Restartable
{
    protected final BigDoors plugin;

    protected Restartable(final BigDoors plugin)
    {
        this.plugin = plugin;
        plugin.registerRestartable(this);
    }

    public abstract void restart();
}
