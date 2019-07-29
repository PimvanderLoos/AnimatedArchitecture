package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SubCommandOpen extends SubCommandToggle
{
    protected static final CommandData command = CommandData.OPEN;
    protected final String help = "Open a door";

    public SubCommandOpen(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean readyToOpen(DoorBase door)
    {
        return (door.getOpenDir().equals(RotateDirection.NONE) || !door.isOpen());
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
            if (readyToOpen(door))
                execute(sender, door, time);
            else
                plugin.getPLogger()
                      .sendMessageToTarget(sender, Level.INFO,
                                           messages.getString(Message.ERROR_DOORALREADYOPEN, door.getName()));
        return doors.size() > 0;
    }
}
