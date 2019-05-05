package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;

/*
 * This class really does whatever I want to test at a given point.
 * I do not guarantee I will clean it up between releases and most
 * definitely do not guarantee that this command will have the same
 * or even remotely similar effects between commits, let alone release.
 * As such, this command should not be used by anyone, hence the weird
 * permission node (so it isn't accidentally included by bigdoors.*).
 * I would hardcode my own username, but I think that that'd be a bit much.
 */
public class SubCommandDebug implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "debug";
    private static final String permission = "bigdoorsdebug.iknowishouldnotusethis";
    private static final String help = "Do not use this unless you are me... What?";
    private static final String argsHelp = null;
    private static final int minArgCount = 1;

    public SubCommandDebug(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public boolean execute(@SuppressWarnings("unused") CommandSender sender)
    {

        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        return execute(sender);
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
