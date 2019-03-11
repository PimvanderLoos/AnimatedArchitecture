package nl.pim16aap2.bigDoors.waitForCommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Abortable;

public abstract class WaitForCommand extends Abortable
{
    protected String command;
    protected Player player;
    protected final BigDoors plugin;

    protected WaitForCommand(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public final void abort(boolean onDisable)
    {
        if (!onDisable)
            plugin.removeCommandWaiter(this);
    }

    public final String getCommand()
    {
        return command;
    }

    public abstract boolean executeCommand(String[] args);

    public final Player getPlayer()
    {
        return player;
    }
}
