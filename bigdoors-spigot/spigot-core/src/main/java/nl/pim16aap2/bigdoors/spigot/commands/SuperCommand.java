package nl.pim16aap2.bigdoors.spigot.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommand;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents a base command that {@link SubCommand}s can register with.
 */
public class SuperCommand implements ICommand
{
    protected final BigDoorsSpigot plugin;
    protected final CommandManager commandManager;
    protected Map<String, SubCommand> subCommands;
    protected int minArgCount;
    protected CommandData command;

    protected SuperCommand(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
        subCommands = new HashMap<>();
    }

    /**
     * Initializes the command.
     *
     * @param minArgCount The minimum number of arguments of the command.
     * @param command     The {@link CommandData} of the command.
     */
    protected final void init(final int minArgCount, final @NonNull CommandData command)
    {
        this.minArgCount = minArgCount;
        this.command = command;
    }

    /**
     * Register a {@link SubCommand} with this {@link SuperCommand}.
     *
     * @param subCommand The {@link SubCommand}.
     */
    public void registerSubCommand(final @NonNull SubCommand subCommand)
    {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        commandManager.registerCommandShortcut(subCommand);
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
               CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        if (args.length == 0 || (args.length == 1 && args[0].toLowerCase().equals("help")))
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, getHelp(sender));
            return true;
        }

        if (args.length == 2 && args[0].toLowerCase().equals("help"))
        {
            SubCommand helpCommand = subCommands.get(args[1].toLowerCase());
            if (helpCommand == null)
                plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
                                                        plugin.getMessages().getString(Message.ERROR_COMMAND_NOTFOUND));
            else
                plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, getHelpOfSubCommand(sender, helpCommand));
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, getHelp(sender));
            return true;
        }
        if (!CommandManager.permissionForCommand(sender, subCommand))
            throw new CommandPermissionException();
        if (args.length < subCommand.getMinArgCount() || !subCommand.onCommand(sender, cmd, label, args))
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, getHelpOfSubCommand(sender, subCommand));
        return true;
    }

    @Override
    public @NonNull String getHelp(final @NonNull CommandSender sender)
    {
        StringBuilder builder = new StringBuilder();
        subCommands.forEach((K, V) ->
                            {
                                if (CommandManager.permissionForCommand(sender, this))
                                {
                                    String help = getHelpOfSubCommand(sender, V);
                                    if (help != null)
                                        builder.append(help);
                                }
                            });
        return builder.toString();
    }

    private @NonNull String getHelpOfSubCommand(final @NonNull CommandSender sender,
                                                final @NonNull SubCommand subCommand)
    {
        String help = subCommand.getHelp(sender);
        String args = subCommand.getHelpArguments();
        return SpigotUtil.helpFormat(getName() + " " + subCommand.getName() + (args == null ? "" : " " + args), help);
    }

    @Override
    public @NonNull String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public @NonNull String getName()
    {
        return CommandData.getCommandName(command);
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @Override
    public @NonNull CommandData getCommandData()
    {
        return command;
    }
}
