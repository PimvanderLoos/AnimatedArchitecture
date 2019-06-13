package nl.pim16aap2.bigdoors.waitforcommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.Util;

public class WaitForSetTime extends WaitForCommand
{
    private final DoorBase door;
    private final SubCommandSetAutoCloseTime subCommand;

    public WaitForSetTime(final BigDoors plugin, final SubCommandSetAutoCloseTime subCommand, final Player player,
        final DoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetTime.Init"));
    }

    @Override
    public boolean executeCommand(String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException, CommandInvalidVariableException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
