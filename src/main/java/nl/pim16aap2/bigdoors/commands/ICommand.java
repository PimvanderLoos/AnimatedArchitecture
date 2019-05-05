package nl.pim16aap2.bigdoors.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public interface ICommand
{
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandInvalidVariableException,
        CommandPlayerNotFoundException, CommandActionNotAllowedException;

    public String getHelp(CommandSender sender);

    public String getPermission();

    public String getName();

    public int getMinArgCount();
}
