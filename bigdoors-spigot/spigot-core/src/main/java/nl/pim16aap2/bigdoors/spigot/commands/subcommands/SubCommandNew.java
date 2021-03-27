package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubCommandNew extends SubCommand
{
    protected static final String help = "Create a new door from selection with name \"doorName\". Defaults to a regular door (BigDoor).";
    protected static final String argsHelp = "[-pc/-db/-bd/-el/-fl/-sd/-gd] <doorName>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.NEW;

    public SubCommandNew(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public static boolean hasCreationPermission(final @NotNull Player player, final @NotNull DoorType type)
    {
        return player.hasPermission(CommandData.getPermission(CommandData.NEW) + type.getSimpleName());
    }

    private boolean isPlayerBusy(final @NotNull Player player)
    {
        boolean isBusy = (BigDoors.get().getToolUserManager().getToolUser(player.getUniqueId()).isPresent() ||
            plugin.getCommandWaiter(player).isPresent());
        if (isBusy)
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_PLAYERISBUSY));
        return isBusy;
    }

    /**
     * Initiates door creation. It is assumed that all checks regarding permissions (allowed to create this type,
     * allowed to create additional doors) are already verified at this point.
     *
     * @param player   The player initiating the door creation.
     * @param name     The name of the door, may be null.
     * @param doorType The type of the door to be created.
     */
    private void initiateDoorCreation(final @NotNull Player player, final @Nullable String name,
                                      final @NotNull DoorType doorType)
    {
        if (name != null && !Util.isValidDoorName(name))
        {
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_INVALIDDOORNAME, name));
            return;
        }

        final @NotNull Creator creator = doorType.getCreator(SpigotAdapter.wrapPlayer(player), name);
        BigDoors.get().getToolUserManager().startToolUser(creator, 120 * 20);
    }

    /**
     * Initiates door creation.
     *
     * @param player       The player initiating the door creation.
     * @param name         The name of the door, may be null.
     * @param doorType     The type of the door to be created.
     * @param maxDoorCount The maximum number of doors this player is allowed to create.
     */
    private void initiateDoorCreation(final @NotNull Player player, final @Nullable String name,
                                      final @NotNull DoorType doorType, final int maxDoorCount)
    {
        if (maxDoorCount < 0)
            initiateDoorCreation(player, name, doorType);
        else
            BigDoors.get().getDatabaseManager().countDoorsOwnedByPlayer(player.getUniqueId()).whenComplete(
                (doorCount, throwable) ->
                {
                    if (doorCount >= maxDoorCount)
                        SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_TOOMANYDOORSOWNED,
                                                                            Integer.toString(maxDoorCount)));
                    else
                        initiateDoorCreation(player, name, doorType);
                });
    }

    // Create a new door.
    public void execute(final @NotNull Player player, final @Nullable String name, final @NotNull DoorType doorType)
    {
        if (!BigDoors.get().getDoorTypeManager().isDoorTypeEnabled(doorType))
        {
            plugin.getPLogger()
                  .severe(
                      "Trying to create door of type: \"" + doorType.toString() + "\", but this type is not enabled!");
            return;
        }

        if (!hasCreationPermission(player, doorType))
        {
            SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_NOPERMISSIONFORDOORTYPE));
            return;
        }

        if (isPlayerBusy(player))
            return;

        SpigotUtil.getMaxDoorsForPlayer(player)
                  .whenComplete((maxDoors, throwable) -> initiateDoorCreation(player, name, doorType, maxDoors));
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        @Nullable DoorType doorType = BigDoors.get().getDoorTypeManager().getDoorType("bigdoor").orElse(null);
        String name = args[args.length - 1];

        if (args.length == minArgCount + 1)
            doorType = BigDoors.get().getDoorTypeManager().getDoorType(args[args.length - 2])
                               .orElse(doorType);
        if (doorType == null)
        {
            sender.sendMessage("TYPE NOT FOUND!");
            return false;
        }

        execute((Player) sender, name, doorType);
        return true;
    }

    @Override
    public @NotNull String getHelp(final @NotNull CommandSender sender)
    {
        return help;
    }

    @Override
    public @NotNull String getHelpArguments()
    {
        return argsHelp;
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @Override
    public @NotNull CommandData getCommandData()
    {
        return command;
    }

    @Override
    public @NotNull String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public @NotNull String getName()
    {
        return CommandData.getCommandName(command);
    }
}
