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
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages all {@link ICommand}s and {@link SubCommand}s.
 *
 * @author Pim
 */
public class CommandManager implements CommandExecutor
{
    private static final String helpMessage = ChatColor.BLUE
        + "{}: Not required when used from GUI, <>: always required, []: optional\n";

    private final BigDoors plugin;
    private Map<String, ICommand> commands;
    private Map<CommandData, ICommand> commandsShortcut;

    public CommandManager(final BigDoors plugin)
    {
        this.plugin = plugin;
        commands = new HashMap<>();
        commandsShortcut = new HashMap<>();
    }

    /**
     * Gets the UUID of a player from an input argument.
     *
     * @param playerArg The player name or UUID.
     * @return The UUID of the player.
     *
     * @throws CommandPlayerNotFoundException If no player was found.
     */
    @NotNull
    public static UUID getPlayerFromArg(@NotNull String playerArg) throws CommandPlayerNotFoundException
    {
        Optional<UUID> playerUUID = SpigotUtil.playerUUIDFromString(playerArg);
        if (!playerUUID.isPresent())
            throw new CommandPlayerNotFoundException(playerArg);
        return playerUUID.get();
    }

    /**
     * Checks if a {@link CommandSender} has access to a {@link ICommand} or not. If the {@link CommandSender}.
     *
     * @param sender  The {@link CommandSender}.
     * @param command The {@link ICommand}.
     * @return True if the {@link CommandSender} has access to the {@link ICommand}.
     */
    public static boolean permissionForCommand(@NotNull CommandSender sender, @NotNull ICommand command)
    {
        return (sender instanceof Player ?
                ((Player) sender).hasPermission(command.getPermission()) || ((Player) sender).isOp() : true);
    }

    /**
     * Gets a long from a String.
     *
     * @param testLong A potential long in String form.
     * @return The long value.
     *
     * @throws IllegalArgumentException If the input argument was not a long.
     */
    public static long getLongFromArg(@NotNull String testLong) throws IllegalArgumentException
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

    /**
     * Gets a integer from a String.
     *
     * @param testInt A potential integer in String form.
     * @return The integer value.
     *
     * @throws IllegalArgumentException If the input argument was not an integer.
     */
    public static int getIntegerFromArg(@NotNull String testInt) throws IllegalArgumentException
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

    /**
     * Gets a float from a String.
     *
     * @param testFloat A potential float in String form.
     * @return The float value.
     *
     * @throws IllegalArgumentException If the input argument was not a float.
     */
    public static float getFloatFromArg(@NotNull String testFloat) throws IllegalArgumentException
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

    /**
     * Gets the help message.
     *
     * @return The help message.
     */
    public static String getHelpMessage()
    {
        return helpMessage;
    }

    /**
     * Registers an {@link ICommand} in this manager.
     *
     * @param command The {@link ICommand}.
     */
    public void registerCommand(final @NotNull ICommand command)
    {
        commands.put(command.getName().toLowerCase(), command);
        commandsShortcut.put(command.getCommandData(), command);
        plugin.getCommand(command.getName()).setExecutor(this);
    }

    /**
     * Registers a shortcut to a {@link SubCommand}, so the whole command tree doesn't have to get traversed to find
     * it.
     *
     * @param subCommand The {@link SubCommand}.
     */
    public void registerCommandShortcut(final @NotNull SubCommand subCommand)
    {
        commandsShortcut.put(subCommand.getCommandData(), subCommand);
    }

    /**
     * Gets an {@link ICommand} that is registered in this class.
     *
     * @param command The {@link CommandData} of the {@link ICommand}.
     * @return The {@link ICommand}.
     */
    @NotNull
    public ICommand getCommand(final @NotNull CommandData command)
    {
        return commandsShortcut.get(command);
    }

    /**
     * Executes a command.
     *
     * @param sender The {@link CommandSender} that executed the command.
     * @param cmd    The command.
     * @param label  The label of the command.
     * @param args   The arguments of the command.
     * @return True if execution of the command was successful.
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
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
                  .sendMessageToTarget(sender, Level.INFO, plugin.getMessages().getString(
                      Message.ERROR_COMMAND_NOTAPLAYER));
        }
        catch (CommandPermissionException e)
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       plugin.getMessages().getString(Message.ERROR_COMMAND_NOPERMISSION));
        }
        catch (IllegalArgumentException e)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED + e.getMessage());
        }
        catch (CommandPlayerNotFoundException e)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, plugin.getMessages()
                                                                              .getString(Message.ERROR_PLAYERNOTFOUND,
                                                                                         e.getPlayerArg()));
        }
        catch (CommandActionNotAllowedException e)
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       plugin.getMessages().getString(Message.ERROR_NOPERMISSIONFORACTION));
        }
        catch (Exception e)
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO, plugin.getMessages().getString(Message.ERROR_GENERALERROR));
            StringBuilder sb = new StringBuilder();
            for (String str : args)
                sb.append(str).append(str.equals(args[args.length - 1]) ? "" : ", ");
            plugin.getPLogger().logException(e, "An exception occurred while processing command \"" + cmd.getName()
                + "\" with args: \"" + sb.toString() + "\"!");
        }
        return true;
    }

    /**
     * Gets the {@link WaitForCommand} of a {@link CommandSender} for a command if one exists.
     *
     * @param sender      The {@link CommandSender}.
     * @param commandName The name of the {@link ICommand}.
     * @return The {@link WaitForCommand} of a {@link CommandSender} for a command if one exists.
     */
    @NotNull
    public Optional<WaitForCommand> isCommandWaiter(final @NotNull CommandSender sender,
                                                    final @NotNull String commandName)
    {
        if (!(sender instanceof Player))
            return Optional.empty();
        return plugin.getCommandWaiter((Player) sender).filter(CW -> CW.getCommand().equals(commandName));
    }

    /**
     * Executes the command of a {@link WaitForCommand}.
     *
     * @param commandWaiter The {@link WaitForCommand}.
     * @param args          The arguments of the command.
     * @param minArgCount   The minimum number of arguments of the command.
     * @return True if command execution was successful.
     *
     * @throws CommandPlayerNotFoundException   When a {@link Player} specified in the arguments was not found.
     * @throws CommandActionNotAllowedException When the action associated with the command was not allowed.
     */
    public boolean commandWaiterExecute(final @NotNull WaitForCommand commandWaiter, final @NotNull String args[],
                                        final int minArgCount)
        throws CommandActionNotAllowedException, CommandPlayerNotFoundException
    {
        commandWaiter.abortSilently();
        if (args.length == minArgCount)
            return commandWaiter.executeCommand(args);
        return false;
    }

    /**
     * Gets the {@link DoorBase} from a String. If the {@link CommandSender} is a {@link Player}, only {@link DoorBase}s
     * owned by them are considered, otherwise all doors are considered and the owner of any of the resulting ones will
     * be the original creator.
     *
     * @param sender  The {@link CommandSender}.
     * @param doorArg The name or UID of the  {@link DoorBase}.
     * @return The {@link DoorBase} if exactly 1 door was found.
     *
     * @throws IllegalArgumentException if more then 1 or exactly 0 doors were found.
     */
    @NotNull
    public DoorBase getDoorFromArg(final @NotNull CommandSender sender, final @NotNull String doorArg)
        throws IllegalArgumentException
    {
        DoorBase door = null;

        if (sender instanceof Player)
            try
            {
                door = plugin.getDatabaseManager().getDoor(((Player) sender).getUniqueId(), doorArg).orElse(null);
            }
            catch (TooManyDoorsException e)
            {
                SpigotUtil.messagePlayer((Player) sender,
                                         plugin.getMessages().getString(Message.ERROR_TOOMANYDOORSFOUND));
            }
            catch (NotEnoughDoorsException e)
            {
                SpigotUtil.messagePlayer((Player) sender, plugin.getMessages().getString(Message.ERROR_NODOORSFOUND));
            }
        else
            try
            {
                door = plugin.getDatabaseManager().getDoor(Long.parseLong(doorArg)).orElse(null);
            }
            catch (NumberFormatException e)
            {
                plugin.getPLogger()
                      .info("\"" + doorArg + "\" " + plugin.getMessages().getString(Message.ERROR_INVALIDDOORID));
            }
        if (door == null)
            throw new IllegalArgumentException("\"" + doorArg + "\" is not a valid door!");
        return door;
    }
}
