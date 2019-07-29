package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class SubCommandSetAutoCloseTime extends SubCommand
{
    protected static final String help = "Changes/Sets time (in s) for doors to automatically close. -1 to disable.";
    protected static final String argsHelp = "{door} <time>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.SETAUTOCLOSETIME;

    public SubCommandSetAutoCloseTime(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, DoorBase door, String timeArg)
        throws CommandActionNotAllowedException, IllegalArgumentException
    {
        if (sender instanceof Player && !plugin.getDatabaseManager()
                                               .hasPermissionForAction((Player) sender, door.getDoorUID(),
                                                                       DoorAttribute.CHANGETIMER))
            throw new CommandActionNotAllowedException();

        int time = CommandManager.getIntegerFromArg(timeArg);

        plugin.getDatabaseManager().setDoorOpenTime(door.getDoorUID(), time);

        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, time < 0 ?
                                                                    messages.getString(Message.COMMAND_SETTIME_SUCCESS,
                                                                                       Integer.toString(time)) :
                                                                    messages.getString(
                                                                        Message.COMMAND_SETTIME_DISABLED));

        @Nullable UUID player = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        plugin.getAutoCloseScheduler().scheduleAutoClose(player, door, time, false);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
               CommandActionNotAllowedException, IllegalArgumentException
    {
        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);
        return execute(sender, commandManager.getDoorFromArg(sender, args[1]), args[2]);
    }
}
