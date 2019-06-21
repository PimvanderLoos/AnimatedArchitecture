package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class SubCommandFill extends SubCommand
{
    protected static final String help = "Replaces all the blocks in this door by stone. Not particularly useful usually.";
    protected static final String argsHelp = "<doorUID>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.FILLDOOR;

    public SubCommandFill(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(DoorBase door)
    {
        plugin.getDatabaseManager().fillDoor(door);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandInvalidVariableException
    {
        return execute(plugin.getDatabaseManager().getDoor(CommandManager.getLongFromArg(args[1])));
    }
}
