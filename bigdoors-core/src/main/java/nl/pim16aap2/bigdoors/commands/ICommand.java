package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public interface ICommand
{
    boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
                   CommandPlayerNotFoundException, CommandActionNotAllowedException;

    String getHelp(CommandSender sender);

    String getPermission();

    String getName();

    int getMinArgCount();

    CommandData getCommandData();
}
