package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;

public class SubCommandCancel extends SubCommand
{
    protected static final String help = "Cancels the current task (e.g. wait for command input)";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.CANCEL;

    public SubCommandCancel(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
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
        {
            tu.abortSilently();
            SpigotUtil.messagePlayer(player,
                                     ChatColor.RED + plugin.getMessages().getString("CREATOR.GENERAL.Cancelled"));
        }
        else
        {
            WaitForCommand cw = plugin.getCommandWaiter(player);
            if (cw != null)
                cw.abortSilently();
        }
        return true;
    }
}
