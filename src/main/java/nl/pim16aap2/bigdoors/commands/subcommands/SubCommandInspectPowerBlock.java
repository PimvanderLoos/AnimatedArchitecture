package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.toolusers.PowerBlockInspector;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;

public class SubCommandInspectPowerBlock implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "inspectpowerblock";
    private static final String permission = "bigdoors.user.inspectpowerblock";
    private static final String help = "Figure out to which door a powerblock location belongs";
    private static final String argsHelp = null;
    private static final int minArgCount = 1;

    public SubCommandInspectPowerBlock(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public boolean execute(Player player)
    {
        plugin.getCommander().startTimerForAbortable(new PowerBlockInspector(plugin, player, -1), 20 * 20);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if ((sender instanceof Player))
            throw new CommandSenderNotPlayerException();
        Player player = (Player) sender;

        ToolUser tu = plugin.getToolUser(player);
        if (tu != null)
            tu.abortSilently();
        execute(player);
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
