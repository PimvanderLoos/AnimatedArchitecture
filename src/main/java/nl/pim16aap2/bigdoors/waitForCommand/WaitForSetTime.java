package nl.pim16aap2.bigdoors.waitForCommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.util.Util;

public class WaitForSetTime extends WaitForCommand
{
    private final Door door;
    private final SubCommandSetAutoCloseTime subCommand;

    public WaitForSetTime(final BigDoors plugin, final SubCommandSetAutoCloseTime subCommand, final Player player,
        final Door door)
    {
        super(plugin);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetTime.Init"));
        plugin.addCommandWaiter(this);
    }

    @Override
    public boolean executeCommand(String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException, CommandInvalidVariableException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }

    @Override
    public String getCommand()
    {
        return subCommand.getName();
    }
}
