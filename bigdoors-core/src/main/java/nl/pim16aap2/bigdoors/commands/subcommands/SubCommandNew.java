package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.toolusers.BigDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.DrawbridgeCreator;
import nl.pim16aap2.bigdoors.toolusers.ElevatorCreator;
import nl.pim16aap2.bigdoors.toolusers.FlagCreator;
import nl.pim16aap2.bigdoors.toolusers.GarageDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.PortcullisCreator;
import nl.pim16aap2.bigdoors.toolusers.RevolvingDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.SlidingDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.toolusers.WindmillCreator;
import nl.pim16aap2.bigdoors.util.Util;

public class SubCommandNew extends SubCommand
{
    protected static final String help = "Create a new door from selection with name \"doorName\". Defaults to a regular door.";
    protected static final String argsHelp = "[-pc/-db/-bd/-el/-fl/-sd/-gd] <doorName>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.NEW;

    public SubCommandNew(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public static boolean hasCreationPermission(Player player, DoorType type)
    {
        return player.hasPermission(CommandData.getPermission(CommandData.NEW) + type.toString().toLowerCase());
    }

    private boolean isPlayerBusy(Player player)
    {
        boolean isBusy = (plugin.getToolUser(player) != null || plugin.isCommandWaiter(player) != null);
        if (isBusy)
            SpigotUtil.messagePlayer(player, plugin.getMessages().getString("GENERAL.IsBusy"));
        return isBusy;
    }

    // Create a new door.
    public void execute(Player player, String name, DoorType type)
    {
        if (!DoorType.isEnabled(type))
        {
            plugin.getMyLogger()
                .severe("Trying to create door of type: \"" + type.toString() + "\", but this type is not enabled!");
            return;
        }

        if (!hasCreationPermission(player, type))
        {
            SpigotUtil
                .messagePlayer(player,
                               ChatColor.RED + plugin.getMessages().getString("GENERAL.NoDoorTypeCreationPermission"));
            return;
        }

        long doorCount = plugin.getDatabaseManager().countDoorsOwnedByPlayer(player.getUniqueId());
        int maxCount = SpigotUtil.getMaxDoorsForPlayer(player);
        if (maxCount >= 0 && doorCount >= maxCount)
        {
            SpigotUtil.messagePlayer(player, ChatColor.RED + plugin.getMessages().getString("GENERAL.TooManyDoors"));
            return;
        }

        if (name != null && !Util.isValidDoorName(name))
        {
            SpigotUtil.messagePlayer(player, ChatColor.RED + "\"" + name + "\""
                + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
            return;
        }

        if (isPlayerBusy(player))
            return;

        ToolUser tu = null;
        switch (type)
        {
        case BIGDOOR:
            tu = new BigDoorCreator(plugin, player, name);
            break;
        case DRAWBRIDGE:
            tu = new DrawbridgeCreator(plugin, player, name);
            break;
        case PORTCULLIS:
            tu = new PortcullisCreator(plugin, player, name);
            break;
        case ELEVATOR:
            tu = new ElevatorCreator(plugin, player, name);
            break;
        case SLIDINGDOOR:
            tu = new SlidingDoorCreator(plugin, player, name);
            break;
        case FLAG:
            tu = new FlagCreator(plugin, player, name);
            break;
        case WINDMILL:
            tu = new WindmillCreator(plugin, player, name);
            break;
        case REVOLVINGDOOR:
            tu = new RevolvingDoorCreator(plugin, player, name);
            break;
        case GARAGEDOOR:
            tu = new GarageDoorCreator(plugin, player, name);
            break;
        default:
            break;
        }

        if (tu == null)
        {
            plugin.getMyLogger()
                .warn("Failed to initiate door creation process for door type: \"" + type.toString() + "\"");
            return;
        }

        plugin.getDatabaseManager().startTimerForAbortable(tu, 60 * 20);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        DoorType type = DoorType.BIGDOOR;
        String name = args[args.length - 1];

        if (args.length == minArgCount + 1)
        {
            type = DoorType.valueOfFlag(args[args.length - 2].toUpperCase());
            if (type == null)
                return false;
        }
        execute((Player) sender, name, type);
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
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @Override
    public CommandData getCommandData()
    {
        return command;
    }

    @Override
    public String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public String getName()
    {
        return CommandData.getCommandName(command);
    }
}
