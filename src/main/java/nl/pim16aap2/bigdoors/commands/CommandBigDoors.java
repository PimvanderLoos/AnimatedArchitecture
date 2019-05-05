package nl.pim16aap2.bigdoors.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;

public class CommandBigDoors extends SuperCommand
{
    private static final String name = "bigdoors";
    private static final String permission = "bigdoors.user";

    public CommandBigDoors(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager, name, permission);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandInvalidVariableException,
        CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        if (args.length == 0 || args[0].toLowerCase().equals("help"))
        {
            plugin.getMyLogger().returnToSender(sender, null, CommandManager.getHelpMessage() + getHelp(sender));
            return true;
        }
        return subCommands.get(args[0].toLowerCase()).onCommand(sender, cmd, label, args);
    }

    @Override
    public int getMinArgCount()
    {
        return 1;
    }
}
