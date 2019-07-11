package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.toolusers.PowerBlockInspector;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubCommandInspectPowerBlock extends SubCommand
{
    protected static final String help = "Figure out to which door a powerblock location belongs";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.INSPECTPOWERBLOCK;

    public SubCommandInspectPowerBlock(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(Player player)
    {
        plugin.getDatabaseManager().startTimerForAbortable(new PowerBlockInspector(plugin, player, -1), 20 * 20);
        return true;
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
            tu.abortSilently();
        execute(player);
        return true;
    }
}
