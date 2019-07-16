package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.ICommand;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.command.CommandSender;

public abstract class SubCommand implements ICommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;
    protected final Messages messages;

    protected String help;
    protected String argsHelp;
    protected int minArgCount;
    protected CommandData command;

    public SubCommand(final BigDoors plugin, CommandManager commandManager)
    {
        this.plugin = plugin;
        messages = plugin.getMessages();
        this.commandManager = commandManager;
    }

    protected final void init(String help, String argsHelp, int minArgCount, CommandData command)
    {
        this.help = help;
        this.argsHelp = argsHelp;
        this.minArgCount = minArgCount;
        this.command = command;
    }

    @Override
    public String getHelp(CommandSender sender)
    {
        return help;
    }

    public String getHelpArguments()
    {
        return argsHelp;
    }

    @Override
    public int getMinArgCount()
    {
        return commandManager.getCommand(CommandData.getSuperCommand(command)).getMinArgCount() + minArgCount;
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
