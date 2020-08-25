package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.EDoorType;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorBigDoor;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorClock;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorDrawbridge;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorElevator;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorFlag;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorGarageDoor;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorPortcullis;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorRevolvingDoor;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorSlidingDoor;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorWindMill;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class SubCommandNew extends SubCommand
{
    protected static final String help = "Create a new door from selection with name \"doorName\". Defaults to a regular door.";
    protected static final String argsHelp = "[-pc/-db/-bd/-el/-fl/-sd/-gd] <doorName>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.NEW;

    private static final Map<EDoorType, String> typePermissions = new EnumMap<>(EDoorType.class);

    static
    {
        for (EDoorType type : EDoorType.cachedValues())
            typePermissions.put(type, CommandData.getPermission(CommandData.NEW) + type.toString().toLowerCase());
    }

    public SubCommandNew(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public static boolean hasCreationPermission(final @NotNull Player player, final @NotNull EDoorType type)
    {
        return player.hasPermission(typePermissions.get(type));
    }

    private boolean isPlayerBusy(final @NotNull Player player)
    {
        boolean isBusy = (ToolUserManager.get().getToolUser(player.getUniqueId()).isPresent() ||
            plugin.getCommandWaiter(player).isPresent());
        if (isBusy)
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_PLAYERISBUSY));
        return isBusy;
    }

    /**
     * Initiates door creation. It is assumed that all checks regarding permissions (allowed to create this type,
     * allowed to create additional doors) are already verified at this point.
     *
     * @param player The player initiating the door creation.
     * @param name   The name of the door, may be null.
     * @param type   The type of the door to be created.
     */
    private void initiateDoorCreation(final @NotNull Player player, final @Nullable String name,
                                      final @NotNull EDoorType type)
    {
        if (name != null && !Util.isValidDoorName(name))
        {
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_INVALIDDOORNAME, name));
            return;
        }

        Creator creator = null;
        IPPlayer pPlayer = SpigotAdapter.wrapPlayer(player);
        switch (type)
        {
            case BIGDOOR:
                creator = new CreatorBigDoor(pPlayer, name);
                break;
            case DRAWBRIDGE:
                creator = new CreatorDrawbridge(pPlayer, name);
                break;
            case PORTCULLIS:
                creator = new CreatorPortcullis(pPlayer, name);
                break;
            case ELEVATOR:
                creator = new CreatorElevator(pPlayer, name);
                break;
            case SLIDINGDOOR:
                creator = new CreatorSlidingDoor(pPlayer, name);
                break;
            case FLAG:
                creator = new CreatorFlag(pPlayer, name);
                break;
            case WINDMILL:
                creator = new CreatorWindMill(pPlayer, name);
                break;
            case REVOLVINGDOOR:
                creator = new CreatorRevolvingDoor(pPlayer, name);
                break;
            case GARAGEDOOR:
                creator = new CreatorGarageDoor(pPlayer, name);
                break;
            case CLOCK:
                creator = new CreatorClock(pPlayer, name);
            default:
                break;
        }

        //noinspection ConstantConditions This check is just here in case new doors are added that do not have creators.
        if (creator == null)
        {
            plugin.getPLogger()
                  .warn("Failed to initiate door creation process for door type: \"" + type.toString() + "\"");
            return;
        }
        ToolUserManager.get().startToolUser(creator, 120 * 20);
    }

    /**
     * Initiates door creation.
     *
     * @param player       The player initiating the door creation.
     * @param name         The name of the door, may be null.
     * @param type         The type of the door to be created.
     * @param maxDoorCount The maximum number of doors this player is allowed to create.
     */
    private void initiateDoorCreation(final @NotNull Player player, final @Nullable String name,
                                      final @NotNull EDoorType type, final int maxDoorCount)
    {
        if (maxDoorCount < 0)
            initiateDoorCreation(player, name, type);
        else
            BigDoors.get().getDatabaseManager().countDoorsOwnedByPlayer(player.getUniqueId()).whenComplete(
                (doorCount, throwable) ->
                {
                    if (doorCount >= maxDoorCount)
                        SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_TOOMANYDOORSOWNED,
                                                                            Integer.toString(maxDoorCount)));
                    else
                        initiateDoorCreation(player, name, type);
                });
    }

    // Create a new door.
    public void execute(final @NotNull Player player, final @Nullable String name, final @NotNull EDoorType type)
    {
        if (!EDoorType.isEnabled(type))
        {
            plugin.getPLogger()
                  .severe("Trying to create door of type: \"" + type.toString() + "\", but this type is not enabled!");
            return;
        }

        if (!hasCreationPermission(player, type))
        {
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_NOPERMISSIONFORDOORTYPE));
            return;
        }

        if (isPlayerBusy(player))
            return;

        SpigotUtil.getMaxDoorsForPlayer(player)
                  .whenComplete((maxDoors, throwable) -> initiateDoorCreation(player, name, type, maxDoors));
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        EDoorType type = EDoorType.BIGDOOR;
        String name = args[args.length - 1];

        if (args.length == minArgCount + 1)
        {
            Optional<EDoorType> optType = EDoorType.valueOfCommandFlag(args[args.length - 2].toUpperCase());
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

    /** {@inheritDoc} */
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
