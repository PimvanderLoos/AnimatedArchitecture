package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.ICommand;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommand;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.exceptions.NotEnoughDoorsException;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class CommandManager implements CommandExecutor
{
    private static final String helpMessage = ChatColor.BLUE
            + "{}: Not required when used from GUI, <>: always required, []: optional\n";

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
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       ChatColor.RED + plugin.getMessages().getString("GENERAL.COMMAND.NotPlayer"));
        }
        catch (CommandPermissionException e)
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       ChatColor.RED + plugin.getMessages().getString("GENERAL.COMMAND.NoPermission"));
        }
        catch (IllegalArgumentException e)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED + e.getMessage());
        }
        catch (CommandPlayerNotFoundException e)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED
                    + plugin.getMessages().getString("GENERAL.PlayerNotFound") + ": \"" + e.getPlayerArg() + "\"");
        }
        catch (CommandActionNotAllowedException e)
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       ChatColor.RED + plugin.getMessages().getString("GENERAL.NoPermissionForAction"));
        }
        catch (Exception e)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
                                                    ChatColor.RED + plugin.getMessages().getString("GENERAL.Error"));
            StringBuilder sb = new StringBuilder();
            for (String str : args)
                sb.append(str + (str.equals(args[args.length - 1]) ? "" : ", "));
            plugin.getPLogger().logException(e, "An exception occurred while processing command \"" + cmd.getName()
                    + "\" with args: \"" + sb.toString() + "\"!");
        }
        return true;
    }

    public DoorBase getDoorFromArg(CommandSender sender, String doorArg) throws IllegalArgumentException
    {
        DoorBase door = null;

        if (sender instanceof Player)
            try
            {
                door = plugin.getDatabaseManager().getDoor(((Player) sender).getUniqueId(), doorArg);
            }
            catch (TooManyDoorsException e)
            {
                SpigotUtil.messagePlayer((Player) sender, plugin.getMessages().getString("GENERAL.MoreThan1DoorFound"));
            }
            catch (NotEnoughDoorsException e)
            {
                SpigotUtil.messagePlayer((Player) sender, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            }
        else
            try
            {
                door = plugin.getDatabaseManager().getDoor(Long.parseLong(doorArg));
            }
            catch (NumberFormatException e)
            {
                plugin.getPLogger()
                      .info("\"" + doorArg + "\" " + plugin.getMessages().getString("GENERAL.InvalidDoorID"));
            }
        if (door == null)
            throw new IllegalArgumentException("\"" + doorArg + "\" is not a valid door!");
        return door;
    }

    public static UUID getPlayerFromArg(String playerArg) throws CommandPlayerNotFoundException
    {
        UUID playerUUID = SpigotUtil.playerUUIDFromString(playerArg);
        if (playerUUID == null)
            throw new CommandPlayerNotFoundException(playerArg);
        return playerUUID;
    }

    public static boolean permissionForCommand(CommandSender sender, ICommand command)
    {
        return (sender instanceof Player ?
                ((Player) sender).hasPermission(command.getPermission()) || ((Player) sender).isOp() : true);
    }

    public static long getLongFromArg(String testLong) throws IllegalArgumentException
    {
        try
        {
            return Long.parseLong(testLong);
        }
        catch (Exception uncaught)
        {
            throw new IllegalArgumentException("\"" + testLong + "\" is not a valid long");
        }
    }

    public static int getIntegerFromArg(String testInt) throws IllegalArgumentException
    {
        try
        {
            return Integer.parseInt(testInt);
        }
        catch (Exception uncaught)
        {
            throw new IllegalArgumentException("\"" + testInt + "\" is not a valid integer");
        }
    }

    public static float getFloatFromArg(String testFloat) throws IllegalArgumentException
    {
        try
        {
            return Float.parseFloat(testFloat);
        }
        catch (Exception uncaught)
        {
            throw new IllegalArgumentException("\"" + testFloat + "\" is not a valid float");
        }
    }

    public static String getHelpMessage()
    {
        return helpMessage;
    }
}
