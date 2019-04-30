package nl.pim16aap2.bigdoors.waitForCommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Abortable;
import nl.pim16aap2.bigdoors.util.Util;

public abstract class WaitForCommand extends Abortable
{
    protected String command;
    protected Player player;
    protected final BigDoors plugin;
    protected boolean isFinished = false;

    protected WaitForCommand(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public final void abort(boolean onDisable)
    {
        if (!onDisable)
        {
            cancelTask();
            plugin.removeCommandWaiter(this);
            if (!isFinished)
                Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.TimeOutOrFail"));
        }
    }

    public final void abortSilently()
    {
        setFinished(true);
        abort();
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

    public void setFinished(boolean finished)
    {
        isFinished = finished;
    }
}
