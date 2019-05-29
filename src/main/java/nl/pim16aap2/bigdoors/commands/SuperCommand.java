package nl.pim16aap2.bigdoors.commands;

import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommand;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.Util;

public class SuperCommand implements ICommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;
    protected HashMap<String, SubCommand> subCommands;
    protected int minArgCount;
    protected CommandData command;

    protected SuperCommand(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
        subCommands = new HashMap<>();
    }

    protected final void init(int minArgCount, CommandData command)
    {
        this.minArgCount = minArgCount;
        this.command = command;
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
            SubCommand helpCommand = subCommands.get(args[1].toLowerCase());
            if (helpCommand == null)
                plugin.getMyLogger().returnToSender(sender, null, plugin.getMessages().getString("GENERAL.COMMAND.NotFound"));
            else
                plugin.getMyLogger().returnToSender(sender, null, getHelpOfSubCommand(sender, helpCommand));
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null)
        {
            plugin.getMyLogger().returnToSender(sender, null, getHelp(sender));
            return true;
        }
        if (!CommandManager.permissionForCommand(sender, subCommand))
            throw new CommandPermissionException();
        if (args.length < subCommand.getMinArgCount() || !subCommand.onCommand(sender, cmd, label, args))
            plugin.getMyLogger().returnToSender(sender, null, getHelpOfSubCommand(sender, subCommand));
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
            return Util.helpFormat(getName() + " " + subCommand.getName() + (args == null ? "" : " " + args), help);
        return null;
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
}
