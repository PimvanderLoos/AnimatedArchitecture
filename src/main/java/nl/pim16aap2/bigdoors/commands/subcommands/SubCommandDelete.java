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
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.util.DoorAttribute;

public class SubCommandDelete implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String help = "Delete the specified door";
    private static final String argsHelp = "<doorUID/Name>";
    private static final int minArgCount = 2;
    private static final CommandData command = CommandData.DELETE;

    public SubCommandDelete(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public boolean execute(CommandSender sender, Door door)
    {
        String name = door.getName();
        long doorUID = door.getDoorUID();
        plugin.getCommander().removeDoor(door.getDoorUID());
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

        try
        {
            door = commandManager.getDoorFromArg(sender, args[1]);
        }
        catch (CommandInvalidVariableException e)
        {
            int count = sender instanceof Player ?
                plugin.getCommander().countDoors(((Player) sender).getUniqueId().toString(), args[1]) :
                plugin.getCommander().countDoors(args[1]);

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
                door = plugin.getCommander().getDoor(args[1], (Player) sender);
            else
                door = plugin.getCommander().getDoor(args[1], null);
        }

        if (sender instanceof Player
            && !plugin.getCommander().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.DELETE))
            throw new CommandActionNotAllowedException();

        if (door == null)
            throw new CommandInvalidVariableException(args[1], "door");

        if (!execute(sender, door))
            return false;
        return true;
    }

    @Override
    public String getHelp(CommandSender sender)
    {
        return help;
    }

    @Override
    public String getHelpArguments()
    {
        return argsHelp;
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @Override
    public CommandData getCommandData()
    {
        return command;
    }

    @Override
    public String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public String getName()
    {
        return CommandData.getCommandName(command);
    }
}
