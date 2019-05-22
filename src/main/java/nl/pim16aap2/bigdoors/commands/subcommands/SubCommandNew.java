package nl.pim16aap2.bigdoors.commands.subcommands;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.toolusers.DoorCreator;
import nl.pim16aap2.bigdoors.toolusers.DrawbridgeCreator;
import nl.pim16aap2.bigdoors.toolusers.ElevatorCreator;
import nl.pim16aap2.bigdoors.toolusers.FlagCreator;
import nl.pim16aap2.bigdoors.toolusers.PortcullisCreator;
import nl.pim16aap2.bigdoors.toolusers.SlidingDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Util;

public class SubCommandNew extends SubCommand
{
    protected static final String help = "Create a new door from selection with name \"doorName\". Defaults to a regular door.";
    protected static final String argsHelp = "[-pc/-db/-bd/-el/-fl] <doorName>";
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
            Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.IsBusy"));
        return isBusy;
    }

    // Create a new door.
    public void execute(Player player, @Nullable String name, DoorType type)
    {
        if (!hasCreationPermission(player, type))
        {
            Util.messagePlayer(player, ChatColor.RED,
                               plugin.getMessages().getString("GENERAL.NoDoorTypeCreationPermission"));
            return;
        }

        long doorCount = plugin.getCommander().countDoors(player.getUniqueId().toString(), null);
        int maxCount = Util.getMaxDoorsForPlayer(player);
        if (maxCount >= 0 && doorCount >= maxCount)
        {
            Util.messagePlayer(player, ChatColor.RED, plugin.getMessages().getString("GENERAL.TooManyDoors"));
            return;
        }

        if (name != null && !Util.isValidDoorName(name))
        {
            Util.messagePlayer(player, ChatColor.RED,
                               "\"" + name + "\"" + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
            return;
        }

        if (isPlayerBusy(player))
            return;

        // These are disabled.
        if (type == DoorType.FLAG)
            return;

        ToolUser tu = type == DoorType.DOOR ? new DoorCreator(plugin, player, name) :
                      type == DoorType.DRAWBRIDGE ? new DrawbridgeCreator(plugin, player, name) :
                      type == DoorType.PORTCULLIS ? new PortcullisCreator(plugin, player, name) :
                      type == DoorType.ELEVATOR ? new ElevatorCreator(plugin, player, name) :
                      type == DoorType.FLAG ? new FlagCreator(plugin, player, name) :
                      type == DoorType.SLIDINGDOOR ? new SlidingDoorCreator(plugin, player, name) : null;

         plugin.getCommander().startTimerForAbortable(tu, 60 * 20);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        DoorType type = DoorType.DOOR;
        String name = args[args.length - 1];

        if (args.length == minArgCount + 1)
        {
            Util.broadcastMessage("TypeStr = " + args[args.length - 2]);
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
