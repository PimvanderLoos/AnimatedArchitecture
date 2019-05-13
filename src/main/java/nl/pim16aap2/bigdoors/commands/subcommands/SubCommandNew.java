package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Util;

public class SubCommandNew implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "new";
    private static final String permission = "bigdoors.user.new";
    private static final String help = "Create a new door from selection with name \"doorName\". Defaults to a regular door.";
    private static final String argsHelp = "[-pc/-db/-bd/-el/-fl] <doorName>";
    private static final int minArgCount = 2;

    public SubCommandNew(final BigDoors plugin, final CommandManager commandManager)
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

        DoorType type = DoorType.DOOR;
        String name = args[args.length - 1];
        Util.broadcastMessage("Name = " + name);
        if (args.length == minArgCount + 1)
        {
            Util.broadcastMessage("TypeStr = " + args[args.length - 2]);
            type = DoorType.valueOfFlag(args[args.length - 2].toUpperCase());
            if (type == null)
                return false;
        }
        plugin.getCommander().startCreator((Player) sender, name, type);
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
