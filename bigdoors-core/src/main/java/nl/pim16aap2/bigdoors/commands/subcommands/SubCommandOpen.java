package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.ArrayList;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class SubCommandOpen extends SubCommandToggle
{
    protected final String help = "Open a door";
    protected static final CommandData command = CommandData.OPEN;

    public SubCommandOpen(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean readyToOpen(DoorBase door)
    {
        return (door.getOpenDir().equals(RotateDirection.NONE) || !door.isOpen());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        ArrayList<DoorBase> doors = new ArrayList<>();
        double time = parseDoorsAndTime(sender, args, doors);

        for (DoorBase door : doors)
            if (readyToOpen(door))
                execute(sender, door, time);
            else
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                    plugin.getMessages().getString("GENERAL.Door") + " \"" + door.getName() +
                                                    "\" " + plugin.getMessages().getString("GENERAL.DoorAlreadyOpen"));
        return doors.size() > 0;
    }
}
