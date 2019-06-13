package nl.pim16aap2.bigdoors.managers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.commands.ICommand;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommand;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.Util;

public class CommandManager implements CommandExecutor
{
    private static final String helpMessage = ChatColor.BLUE + "{}: Not required when used from GUI, <>: always required, []: optional\n";

    private final BigDoors plugin;
    private HashMap<String, ICommand> commands;
    private HashMap<CommandData, ICommand> commandsShortcut;

    public CommandManager(final BigDoors plugin)
    {
        this.plugin = plugin;
        commands = new HashMap<>();
        commandsShortcut = new HashMap<>();
    }

    public void registerCommand(ICommand command)
    {
        commands.put(command.getName().toLowerCase(), command);
        commandsShortcut.put(command.getCommandData(), command);
        plugin.getCommand(command.getName()).setExecutor(this);
    }

    public void registerCommandShortcut(SubCommand subCommand)
    {
        commandsShortcut.put(subCommand.getCommandData(), subCommand);
    }

    public ICommand getCommand(String name)
    {
        return commands.get(name);
    }

    public ICommand getCommand(CommandData command)
    {
        return commandsShortcut.get(command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        ICommand command = commands.get(cmd.getName().toLowerCase());
        try
        {
            if (!permissionForCommand(sender, command))
                throw new CommandPermissionException();
            return command.onCommand(sender, cmd, label, args);
        }
        catch (CommandSenderNotPlayerException e)
        {
            plugin.getMyLogger().returnToSender(sender, ChatColor.RED,
                                                plugin.getMessages().getString("GENERAL.COMMAND.NotPlayer"));
        }
        catch (CommandPermissionException e)
        {
            plugin.getMyLogger().returnToSender(sender, ChatColor.RED,
                                                plugin.getMessages().getString("GENERAL.COMMAND.NoPermission"));
        }
        catch (CommandInvalidVariableException e)
        {
            plugin.getMyLogger().returnToSender(sender, ChatColor.RED, e.getMessage());
        }
        catch (CommandPlayerNotFoundException e)
        {
            plugin.getMyLogger().returnToSender(sender, ChatColor.RED,
                                                plugin.getMessages().getString("GENERAL.PlayerNotFound") + ": \""
                                                    + e.getPlayerArg() + "\"");
        }
        catch (CommandActionNotAllowedException e)
        {
            plugin.getMyLogger().returnToSender(sender, ChatColor.RED,
                                                plugin.getMessages().getString("GENERAL.NoPermissionForAction"));
        }
        return true;
    }

    public DoorBase getDoorFromArg(CommandSender sender, String doorArg) throws CommandInvalidVariableException
    {
        DoorBase door = null;

        if (sender instanceof Player)
            door = plugin.getDatabaseManager().getDoor(doorArg, (Player) sender);
        else
            try
            {
                long doorUID = Long.parseLong(doorArg);
                door = plugin.getDatabaseManager().getDoor(doorUID);
            }
            catch (NumberFormatException e)
            {}
        if (door == null)
            throw new CommandInvalidVariableException(doorArg, "door");
        return door;
    }

    public static UUID getPlayerFromArg(String playerArg) throws CommandPlayerNotFoundException
    {
        UUID playerUUID = Util.playerUUIDFromString(playerArg);
        if (playerUUID == null)
            throw new CommandPlayerNotFoundException(playerArg);
        return playerUUID;
    }

    public static boolean permissionForCommand(CommandSender sender, ICommand command)
    {
        return (sender instanceof Player ? ((Player) sender).hasPermission(command.getPermission())
            || ((Player) sender).isOp() : true);
    }

    public static long getLongFromArg(String timeArg) throws CommandInvalidVariableException
    {
        try
        {
            return Long.parseLong(timeArg);
        }
        catch (Exception uncaught)
        {
            throw new CommandInvalidVariableException(timeArg, "long");
        }
    }

    public static int getIntegerFromArg(String timeArg) throws CommandInvalidVariableException
    {
        try
        {
            return Integer.parseInt(timeArg);
        }
        catch (Exception uncaught)
        {
            throw new CommandInvalidVariableException(timeArg, "integer");
        }
    }

    public static float getFloatFromArg(String timeArg) throws CommandInvalidVariableException
    {
        try
        {
            return Float.parseFloat(timeArg);
        }
        catch (Exception uncaught)
        {
            throw new CommandInvalidVariableException(timeArg, "float");
        }
    }

    public static String getHelpMessage()
    {
        return helpMessage;
    }
}
