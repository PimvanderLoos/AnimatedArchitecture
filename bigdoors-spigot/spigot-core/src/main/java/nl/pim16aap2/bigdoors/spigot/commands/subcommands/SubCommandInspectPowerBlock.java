package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubCommandInspectPowerBlock extends SubCommand
{
    protected static final String help = "Figure out to which door a powerblock location belongs";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.INSPECTPOWERBLOCK;

    public SubCommandInspectPowerBlock(final @NonNull BigDoorsSpigot plugin,
                                       final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(final @NonNull Player player)
    {
//        plugin.getAbortableTaskManager()
//              .startTimerForAbortableTask(new PowerBlockInspector(plugin, player, -1), 20 * 20);
        return true;
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws CommandSenderNotPlayerException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();
        Player player = (Player) sender;

//        plugin.getToolUser(player).ifPresent(ToolUser::abortSilently);
        execute(player);
        return true;
    }
}
