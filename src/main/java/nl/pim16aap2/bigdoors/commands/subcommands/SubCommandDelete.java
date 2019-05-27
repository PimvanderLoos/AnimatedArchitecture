package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;

public class SubCommandDelete extends SubCommand
{
    protected static final String help = "Delete the specified door";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.DELETE;

    public SubCommandDelete(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, Door door)
    {
        String name = door.getName();
        long doorUID = door.getDoorUID();
        plugin.getDatabaseManager().removeDoor(door.getDoorUID());
        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                            plugin.getMessages().getString("GENERAL.COMMAND.DoorIsDeleted") + " " + name
                                                + " (" + doorUID + ")");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandInvalidVariableException,
        CommandActionNotAllowedException
    {
        Door door = null;
        String doorArg = args[getMinArgCount() - 1];
        try
        {
            door = commandManager.getDoorFromArg(sender, doorArg);
        }
        catch (CommandInvalidVariableException e)
        {
            int count = sender instanceof Player ?
                plugin.getDatabaseManager().countDoors(((Player) sender).getUniqueId().toString(), doorArg) :
                plugin.getDatabaseManager().countDoors(doorArg);

            if (count > 1)
            {
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                    plugin.getMessages().getString("GENERAL.MoreThan1DoorFound"));
                return true;
            }
            if (count < 1)
            {
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                    plugin.getMessages().getString("GENERAL.NoDoorsFound"));
                return true;
            }

            if (sender instanceof Player)
                door = plugin.getDatabaseManager().getDoor(doorArg, (Player) sender);
            else
                door = plugin.getDatabaseManager().getDoor(doorArg, null);
        }

        if (sender instanceof Player
            && !plugin.getDatabaseManager().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.DELETE))
            throw new CommandActionNotAllowedException();

        if (door == null)
            throw new CommandInvalidVariableException(doorArg, "door");

        if (!execute(sender, door))
            return false;
        return true;
    }
}
