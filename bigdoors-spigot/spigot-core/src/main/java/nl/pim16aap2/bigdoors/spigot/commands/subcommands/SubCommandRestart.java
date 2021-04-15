package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class SubCommandRestart extends SubCommand
{
    protected static final String help = "Restart the plugin. Reinitializes almost everything.";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 0;
    protected static final CommandData command = CommandData.RESTART;

    public SubCommandRestart(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        plugin.restart();
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, "BigDoors has been restarted!");
        return true;
    }
}
