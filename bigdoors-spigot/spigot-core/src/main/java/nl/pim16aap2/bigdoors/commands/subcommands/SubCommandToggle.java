package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionEventSpigot;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SubCommandToggle extends SubCommand
{
    protected final String help = "Toggle a door";
    protected final String argsHelp = "<doorUID/Name1> <doorUID/Name2> ... [time (decimal!)]";
    protected final int minArgCount = 2;
    protected final CommandData command = CommandData.TOGGLE;
    protected DoorActionType actionType = DoorActionType.TOGGLE;

    public SubCommandToggle(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public void execute(final @NotNull CommandSender sender, final @NotNull DoorBase door)
    {
        execute(sender, door, 0.0D);
    }

    private void toggleDoor(final @NotNull CommandSender sender, final @NotNull DoorBase door, final double time)
    {
        UUID playerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        // TODO: Less stupid system.
        CompletableFuture<Optional<DoorBase>> futureDoor = CompletableFuture.completedFuture(Optional.of(door));

        plugin.callDoorActionEvent(playerUUID == null ?
                                   new DoorActionEventSpigot(futureDoor, DoorActionCause.SERVER, actionType,
                                                             door.getPlayerUUID(), time) :
                                   new DoorActionEventSpigot(futureDoor, DoorActionCause.PLAYER, actionType, playerUUID,
                                                             time));
    }

    public void execute(final @NotNull CommandSender sender, final @NotNull DoorBase door, final double time)
    {
        if (!(sender instanceof Player))
        {
            toggleDoor(sender, door, time);
            return;
        }
        plugin.getDatabaseManager().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.TOGGLE)
              .whenComplete(
                  (isAllowed, throwable) ->
                  {
                      if (!isAllowed)
                          commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
                      else
                          toggleDoor(sender, door, time);
                      // No need to print result message here, that'll be done by the opening process of the door itself.
                  });
    }

    @NotNull
    private CompletableFuture<Double> parseDoorsAndTime(final @NotNull CommandSender sender,
                                                        final @NotNull String[] args,
                                                        final @NotNull List<DoorBase> doors)
        throws IllegalArgumentException
    {
        final String lastStr = args[args.length - 1];
        // First try to get a long from the last string. If it's successful, it must be a door UID.
        // If it isn't successful (-1), try to get parse it as a double. If that is successful, it
        // must be the speed. If that isn't successful either (0.0), it must be a door name.
        long lastUID = Util.longFromString(lastStr, -1L);
        double time = lastUID == -1L ? Util.doubleFromString(lastStr, 0.0D) : 0.0D;
        int index = args.length;
        // If the time variable was specified, decrement endIDX by 1, as the last argument is not a door!
        if (time != 0.0D)
            --index;
        final int doorCount = index;

        return CompletableFuture.supplyAsync(
            () ->
            {
                int currentPos = doorCount;
                while (currentPos-- > 1)
                {
                    try
                    {
                        commandManager.getDoorFromArg(sender, args[currentPos], null, null).get().ifPresent(doors::add);
                    }
                    catch (InterruptedException | ExecutionException e)
                    {
                        plugin.getPLogger().logException(e, "Failed to obtain door \"" + args[currentPos] + "\"");
                    }
                }
                return time;
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws IllegalArgumentException
    {
        final List<DoorBase> doors = new ArrayList<>();
        parseDoorsAndTime(sender, args, doors).whenComplete(
            (time, throwable) ->
            {
                for (DoorBase door : doors)
                    execute(sender, door, time);
            });
        return true;
    }
}
