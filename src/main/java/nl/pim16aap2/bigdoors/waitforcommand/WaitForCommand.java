package nl.pim16aap2.bigdoors.waitforcommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommand;
import nl.pim16aap2.bigdoors.util.Abortable;
import nl.pim16aap2.bigdoors.util.Util;

public abstract class WaitForCommand extends Abortable
{
    protected Player player;
    protected final BigDoors plugin;
    protected boolean isFinished = false;
    protected final SubCommand subCommand;

    protected WaitForCommand(final BigDoors plugin, final SubCommand subCommand)
    {
        this.plugin = plugin;
        this.subCommand = subCommand;
        plugin.addCommandWaiter(this);
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

    @Override
    public final void abortSilently()
    {
        setFinished(true);
        abort();
    }

    public final String getCommand()
    {
        return subCommand.getName();
    }

    public abstract boolean executeCommand(String[] args) throws CommandPlayerNotFoundException, CommandActionNotAllowedException, CommandInvalidVariableException;

    public final Player getPlayer()
    {
        return player;
    }

    public void setFinished(boolean finished)
    {
        isFinished = finished;
    }
}
