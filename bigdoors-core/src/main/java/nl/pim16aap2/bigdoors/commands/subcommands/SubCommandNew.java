package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.toolusers.BigDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.Creator;
import nl.pim16aap2.bigdoors.toolusers.DrawbridgeCreator;
import nl.pim16aap2.bigdoors.toolusers.ElevatorCreator;
import nl.pim16aap2.bigdoors.toolusers.FlagCreator;
import nl.pim16aap2.bigdoors.toolusers.GarageDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.PortcullisCreator;
import nl.pim16aap2.bigdoors.toolusers.RevolvingDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.SlidingDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.WindmillCreator;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SubCommandNew extends SubCommand
{
    protected static final String help = "Create a new door from selection with name \"doorName\". Defaults to a regular door.";
    protected static final String argsHelp = "[-pc/-db/-bd/-el/-fl/-sd/-gd] <doorName>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.NEW;

    public SubCommandNew(final @NotNull BigDoors plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public static boolean hasCreationPermission(final @NotNull Player player, final @NotNull DoorType type)
    {
        return player.hasPermission(CommandData.getPermission(CommandData.NEW) + type.toString().toLowerCase());
    }

    private boolean isPlayerBusy(final @NotNull Player player)
    {
        boolean isBusy = (plugin.getToolUser(player).isPresent() || plugin.getCommandWaiter(player).isPresent());
        if (isBusy)
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_PLAYERISBUSY));
        return isBusy;
    }

    // Create a new door.
    public void execute(final @NotNull Player player, final @Nullable String name, final @NotNull DoorType type)
    {
        if (!DoorType.isEnabled(type))
        {
            plugin.getPLogger()
                  .severe("Trying to create door of type: \"" + type.toString() + "\", but this type is not enabled!");
            return;
        }

        if (!hasCreationPermission(player, type))
        {
            SpigotUtil
                .messagePlayer(player, messages.getString(Message.ERROR_NOPERMISSIONFORDOORTYPE));
            return;
        }

        long doorCount = plugin.getDatabaseManager().countDoorsOwnedByPlayer(player.getUniqueId());
        int maxCount = SpigotUtil.getMaxDoorsForPlayer(player);
        if (maxCount >= 0 && doorCount >= maxCount)
        {
            SpigotUtil.messagePlayer(player,
                                     messages.getString(Message.ERROR_TOOMANYDOORSOWNED, Integer.toString(maxCount)));
            return;
        }

        if (name != null && !Util.isValidDoorName(name))
        {
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_INVALIDDOORNAME, name));
            return;
        }

        if (isPlayerBusy(player))
            return;

        Creator creator = null;
        switch (type)
        {
            case BIGDOOR:
                creator = new BigDoorCreator(plugin, player, name);
                break;
            case DRAWBRIDGE:
                creator = new DrawbridgeCreator(plugin, player, name);
                break;
            case PORTCULLIS:
                creator = new PortcullisCreator(plugin, player, name);
                break;
            case ELEVATOR:
                creator = new ElevatorCreator(plugin, player, name);
                break;
            case SLIDINGDOOR:
                creator = new SlidingDoorCreator(plugin, player, name);
                break;
            case FLAG:
                creator = new FlagCreator(plugin, player, name);
                break;
            case WINDMILL:
                creator = new WindmillCreator(plugin, player, name);
                break;
            case REVOLVINGDOOR:
                creator = new RevolvingDoorCreator(plugin, player, name);
                break;
            case GARAGEDOOR:
                creator = new GarageDoorCreator(plugin, player, name);
                break;
            default:
                break;
        }

        if (creator == null)
        {
            plugin.getPLogger()
                  .warn("Failed to initiate door creation process for door type: \"" + type.toString() + "\"");
            return;
        }

        plugin.getDatabaseManager().startTimerForAbortableTask(creator, 60 * 20);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        DoorType type = DoorType.BIGDOOR;
        String name = args[args.length - 1];

        if (args.length == minArgCount + 1)
        {
            Optional<DoorType> optType = DoorType.valueOfCommandFlag(args[args.length - 2].toUpperCase());
            if (!optType.isPresent())
                return false;
            else type = optType.get();
        }
        execute((Player) sender, name, type);
        return true;
    }

    @Override
    @NotNull
    public String getHelp(final @NotNull CommandSender sender)
    {
        return help;
    }

    @Override
    @NotNull
    public String getHelpArguments()
    {
        return argsHelp;
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @NotNull
    @Override
    public CommandData getCommandData()
    {
        return command;
    }

    @Override
    @NotNull
    public String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    @NotNull
    public String getName()
    {
        return CommandData.getCommandName(command);
    }
}
