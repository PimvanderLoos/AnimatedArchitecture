package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;

public class SubCommandNew implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "";
    private static final String permission = "";
    private static final String help = "";
    private static final String argsHelp = null;
    private static final int minArgCount = 0;

    public SubCommandNew(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
        todo
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {

        return true;
    }

    @Override
    public String getHelp(CommandSender sender)
    {
        return help;
    }

    @Override
    public String getHelpArguments()
    {
        return argsHelp;
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

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }
}
