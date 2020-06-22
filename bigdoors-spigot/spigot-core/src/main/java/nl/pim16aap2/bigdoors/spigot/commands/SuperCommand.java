package nl.pim16aap2.bigdoors.spigot.commands;

import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommand;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

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

    protected SuperCommand(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
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
    protected final void init(final int minArgCount, final @NotNull CommandData command)
    {
        this.minArgCount = minArgCount;
        this.command = command;
    }

    /**
     * Register a {@link SubCommand} with this {@link SuperCommand}.
     *
     * @param subCommand The {@link SubCommand}.
     */
    public void registerSubCommand(final @NotNull SubCommand subCommand)
    {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        commandManager.registerCommandShortcut(subCommand);
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
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

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getHelp(final @NotNull CommandSender sender)
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

    /** {@inheritDoc} */
    @NotNull
    private String getHelpOfSubCommand(final @NotNull CommandSender sender, final @NotNull SubCommand subCommand)
    {
        String help = subCommand.getHelp(sender);
        String args = subCommand.getHelpArguments();
        return SpigotUtil.helpFormat(getName() + " " + subCommand.getName() + (args == null ? "" : " " + args), help);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getPermission()
    {
        return CommandData.getPermission(command);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getName()
    {
        return CommandData.getCommandName(command);
    }

    /** {@inheritDoc} */
    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public CommandData getCommandData()
    {
        return command;
    }
}
