package nl.pim16aap2.bigdoors.commands;

import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.ISubCommand;
import nl.pim16aap2.bigdoors.util.Util;

public abstract class SuperCommand implements ICommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private final String name;
    private final String permission;
    protected HashMap<String, ISubCommand> subCommands;

    public SuperCommand(final BigDoors plugin, final CommandManager commandManager, final String name,
        final String permission)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.name = name;
        this.permission = permission;
        subCommands = new HashMap<>();
    }

    public void registerSubCommand(ISubCommand subCommand)
    {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    public ICommand getCommand(String name)
    {
        return subCommands.get(name);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandInvalidVariableException,
        CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        if (args.length == 0)
            plugin.getMyLogger().returnToSender(sender, null, getHelp(sender));
        else
        {
            ISubCommand command = subCommands.get(args[0].toLowerCase());
            if (!CommandManager.permissionForCommand(sender, command))
                throw new CommandPermissionException();
            if (args.length < command.getMinArgCount() || !command.onCommand(sender, cmd, label, args))
                plugin.getMyLogger().returnToSender(sender, null, getHelpOfSubCommand(sender, command));
        }
        return true;
    }

    @Override
    public String getHelp(CommandSender sender)
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

    private String getHelpOfSubCommand(CommandSender sender, ISubCommand subCommand)
    {
        String help = subCommand.getHelp(sender);
        String args = subCommand.getHelpArguments();
        if (help != null)
            return Util.helpFormat(name + " " + subCommand.getName() + (args == null ? "" : " " + args), help);
        return null;
    }

    @Override
    public String getPermission()
    {
        return permission;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
