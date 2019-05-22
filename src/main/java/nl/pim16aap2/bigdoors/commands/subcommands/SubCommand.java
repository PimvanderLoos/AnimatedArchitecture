package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.ICommand;

public abstract class SubCommand implements ICommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    protected String help;
    protected String argsHelp;
    protected int minArgCount;
    protected CommandData command;

    public SubCommand(final BigDoors plugin, CommandManager commandManager)
    {
        this.plugin = plugin;
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
