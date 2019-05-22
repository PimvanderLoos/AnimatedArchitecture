package nl.pim16aap2.bigdoors.commands;

import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommand;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.Util;

public abstract class SuperCommand implements ICommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private final String name;
    private final String permission;
    protected HashMap<String, SubCommand> subCommands;

    public SuperCommand(final BigDoors plugin, final CommandManager commandManager, final String name,
        final String permission)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.name = name;
        this.permission = permission;
        subCommands = new HashMap<>();
    }

    public void registerSubCommand(SubCommand subCommand)
    {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        commandManager.registerCommandShortcut(subCommand);
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
        if (args.length == 0 || (args.length == 1 && args[0].toLowerCase().equals("help")))
        {
            plugin.getMyLogger().returnToSender(sender, null, getHelp(sender));
            return true;
        }

        if (args.length == 2 && args[0].toLowerCase().equals("help"))
        {
            SubCommand command = subCommands.get(args[1].toLowerCase());
            if (command == null)
                plugin.getMyLogger().returnToSender(sender, null, plugin.getMessages().getString("GENERAL.COMMAND.NotFound"));
            else
                plugin.getMyLogger().returnToSender(sender, null, getHelpOfSubCommand(sender, command));
            return true;
        }

        SubCommand command = subCommands.get(args[0].toLowerCase());
        if (command == null)
            return false;
        if (!CommandManager.permissionForCommand(sender, command))
            throw new CommandPermissionException();
        if (args.length < command.getMinArgCount() || !command.onCommand(sender, cmd, label, args))
            plugin.getMyLogger().returnToSender(sender, null, getHelpOfSubCommand(sender, command));
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

    private String getHelpOfSubCommand(CommandSender sender, SubCommand subCommand)
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
