package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.toolUsers.ToolUser;
import nl.pim16aap2.bigdoors.waitForCommand.WaitForCommand;

public class SubCommandCancel implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "cancel";
    private static final String permission = "bigdoors.user";
    private static final String help = "Cancels the current task (e.g. wait for command input)";
    private static final String argsHelp = null;
    private static final int minArgCount = 1;

    public SubCommandCancel(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();
        Player player = (Player) sender;
        ToolUser tu = plugin.getToolUser(player);
        if (tu != null)
            tu.abort();
        else
        {
            WaitForCommand cw = plugin.getCommandWaiter(player);
            if (cw != null)
                cw.abort();
        }
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
