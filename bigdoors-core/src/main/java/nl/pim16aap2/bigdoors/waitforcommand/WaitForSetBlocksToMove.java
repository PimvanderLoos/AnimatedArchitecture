package nl.pim16aap2.bigdoors.waitforcommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;

public class WaitForSetBlocksToMove extends WaitForCommand
{
    private final DoorBase door;
    private final SubCommandSetBlocksToMove subCommand;

    public WaitForSetBlocksToMove(final BigDoors plugin, final SubCommandSetBlocksToMove subCommand,
        final Player player, final DoorBase door)
    {
        super(plugin, subCommand);
        this.subCommand = subCommand;
        this.player = player;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetBlocksToMove.Init"));
    }

    @Override
    public boolean executeCommand(String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException, CommandInvalidVariableException
    {
//        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
