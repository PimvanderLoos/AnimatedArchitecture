package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.toolusers.PowerBlockInspector;
import nl.pim16aap2.bigdoors.spigot.toolusers.ToolUser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SubCommandInspectPowerBlock extends SubCommand
{
    protected static final String help = "Figure out to which door a powerblock location belongs";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.INSPECTPOWERBLOCK;

    public SubCommandInspectPowerBlock(final @NotNull BigDoorsSpigot plugin,
                                       final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(final @NotNull Player player)
    {
        plugin.getAbortableTaskManager()
              .startTimerForAbortableTask(new PowerBlockInspector(plugin, player, -1), 20 * 20);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();
        Player player = (Player) sender;

        plugin.getToolUser(player).ifPresent(ToolUser::abortSilently);
        execute(player);
        return true;
    }
}
