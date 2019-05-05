package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.toolUsers.ToolUser;
import nl.pim16aap2.bigdoors.util.Util;

public class SubCommandNameDoor implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "namedoor";
    private static final String permission = "bigdoors.user.createdoor";
    private static final String help = "Set the name of the door in the door creation process.";
    private static final String argsHelp = "<doorName>";
    private static final int minArgCount = 2;

    public SubCommandNameDoor(final BigDoors plugin, final CommandManager commandManager)
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
        {
            if (args.length == 1 && Util.isValidDoorName(args[0]))
            {
                tu.setName(args[0]);
                return true;
            }
            return false;
        }
        Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.NotBusy"));
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
