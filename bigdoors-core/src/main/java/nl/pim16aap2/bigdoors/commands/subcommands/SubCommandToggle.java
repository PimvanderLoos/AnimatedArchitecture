package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class SubCommandToggle extends SubCommand
{
    protected final String help = "Toggle a door";
    protected final String argsHelp = "<doorUID/Name1> <doorUID/Name2> ... [time (decimal!)]";
    protected final int minArgCount = 2;
    protected final CommandData command = CommandData.TOGGLE;

    public SubCommandToggle(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public void execute(CommandSender sender, DoorBase door)
    {
        execute(sender, door, 0.0D);
    }

    public void execute(CommandSender sender, DoorBase door, double time)
    {
        if (sender instanceof Player && !plugin.getDatabaseManager()
                                               .hasPermissionForAction((Player) sender, door.getDoorUID(),
                                                                       DoorAttribute.TOGGLE))
            return;

        UUID playerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        // Get a new instance of the door to make sure the locked / unlocked status is
        // recent.
        Optional<DoorBase> newDoor = plugin.getDatabaseManager().getDoor(playerUUID, door.getDoorUID());

        if (!newDoor.isPresent())
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       messages.getString(Message.ERROR_TOGGLEFAILURE, door.getName()));
            return;
        }
        if (newDoor.get().isLocked())
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       messages.getString(Message.ERROR_DOORISLOCKED, door.getName()));

        else
        {
            DoorToggleResult result = playerUUID == null ?
                                      door.toggle(DoorActionCause.SERVER, door.getPlayerUUID(), time, false) :
                                      door.toggle(DoorActionCause.PLAYER, playerUUID, time, false);

            // If it's a success, there's no need to inform the user. If they didn't have permission for the location,
            // they'll already have received a message.
            if (!result.equals(DoorToggleResult.SUCCESS) && !result.equals(DoorToggleResult.NOPERMISSION))
            {
                plugin.getPLogger()
                      .sendMessageToTarget(sender, Level.INFO,
                                           messages.getString(DoorToggleResult.getMessage(result),
                                                              Long.toString(door.getDoorUID())));
            }
        }
    }

    double parseDoorsAndTime(CommandSender sender, String[] args, List<DoorBase> doors)
        throws IllegalArgumentException
    {
        String lastStr = args[args.length - 1];
        // First try to get a long from the last string. If it's successful, it must be a door UID.
        // If it isn't successful (-1), try to get parse it as a double. If that is successful, it
        // must be the speed. If that isn't successful either (0.0), it must be a door name.
        long lastUID = Util.longFromString(lastStr, -1L);
        double time = lastUID == -1L ? Util.doubleFromString(lastStr, 0.0D) : 0.0D;
        int index = args.length;
        // If the time variable was specified, decrement endIDX by 1, as the last argument is not a door!
        if (time != 0.0D)
            --index;

        while (index-- > 1)
            doors.add(commandManager.getDoorFromArg(sender, args[index]));
        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException
    {
        List<DoorBase> doors = new ArrayList<>();
        double time = parseDoorsAndTime(sender, args, doors);

        for (DoorBase door : doors)
            execute(sender, door, time);
        return doors.size() > 0;
    }
}
