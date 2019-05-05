package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;

public class SubCommandVersion implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "version";
    private static final String permission = "bigdoors.admin.version";
    private static final String help = "Get the version of this plugin.";
    private static final String argsHelp = null;
    private static final int minArgCount = 1;

    public SubCommandVersion(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        plugin.getMyLogger()
            .returnToSender(sender, Level.INFO, ChatColor.GREEN,
                            "This server uses version " + plugin.getDescription().getVersion() + " of this plugin!");
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
