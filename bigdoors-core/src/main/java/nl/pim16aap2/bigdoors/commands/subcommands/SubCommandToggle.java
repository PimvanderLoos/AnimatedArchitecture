package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.moveblocks.Opener;
import nl.pim16aap2.bigdoors.spigotutil.DoorAttribute;
import nl.pim16aap2.bigdoors.spigotutil.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.Util;

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
        if (sender instanceof Player
            && !plugin.getDatabaseManager().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.TOGGLE))
            return;

        UUID playerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        // Get a new instance of the door to make sure the locked / unlocked status is
        // recent.
        DoorBase newDoor = plugin.getDatabaseManager().getDoor(playerUUID, door.getDoorUID());

        if (newDoor == null)
        {
            plugin.getMyLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED +
                                                plugin.getMessages().getString("GENERAL.ToggleFailure"));
            return;
        }
        if (newDoor.isLocked())
            plugin.getMyLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED +
                                                plugin.getMessages().getString("GENERAL.DoorIsLocked"));

        else
        {
            Opener opener = plugin.getDoorOpener(newDoor.getType());
            DoorOpenResult result = opener == null ? DoorOpenResult.TYPEDISABLED : opener.openDoor(newDoor, time);

            if (result != DoorOpenResult.SUCCESS)
                plugin.getMyLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED +
                                                    plugin.getMessages().getString(DoorOpenResult.getMessage(result)));
        }
    }

    public double parseDoorsAndTime(CommandSender sender, String[] args, ArrayList<DoorBase> doors)
    {
        String lastStr = args[args.length - 1];
        // Last argument sets speed if it's a double.
        double time = Util.longFromString(lastStr, -1L) == -1L ? Util.doubleFromString(lastStr, 0.0D) : 0.0D;
        int index = args.length;
        // If the time variable was specified, decrement endIDX by 1, as the last
        // argument is not a door!
        if (time != 0.0D)
            --index;

        Player player = sender instanceof Player ? (Player) sender : null;
        while (index --> 1)
        {
            DoorBase door = plugin.getDatabaseManager().getDoor(args[index], player);
            if (door == null)
            {
                plugin.getMyLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED + "\"" + args[index] + "\" "
                    + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
                continue;
            }
            doors.add(door);
        }
        return time;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        ArrayList<DoorBase> doors = new ArrayList<>();
        double time = parseDoorsAndTime(sender, args, doors);

        for (DoorBase door : doors)
            execute(sender, door, time);
        return doors.size() > 0;
    }
}
