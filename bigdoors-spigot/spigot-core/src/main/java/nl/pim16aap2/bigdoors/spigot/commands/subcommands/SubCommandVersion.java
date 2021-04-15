package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class SubCommandVersion extends SubCommand
{
    protected static final String help = "Get the version of this plugin.";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.VERSION;

    public SubCommandVersion(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
    {
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, "This server uses version "
            + plugin.getDescription().getVersion() + " of this plugin!");
        return true;
    }
}
