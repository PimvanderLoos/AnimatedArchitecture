package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.moveBlocks.Opener;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.Util;

public class SubCommandToggle implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private final String name = "toggle";
    private final String permission = "bigdoors.user.toggle";
    private final String help = "Toggle a door";
    protected static final String argsHelp = "<doorUUID/Name1> <doorUUID/Name2> ... [time (decimal!)]";
    protected static final int minArgCount = 2;

    public SubCommandToggle(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public void toggleDoorCommand(CommandSender sender, Door door, double time)
    {
        if (sender instanceof Player
            && !plugin.getCommander().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.TOGGLE))
            return;

        UUID playerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        // Get a new instance of the door to make sure the locked / unlocked status is
        // recent.
        Door newDoor = plugin.getCommander().getDoor(playerUUID, door.getDoorUID());

        if (newDoor == null)
        {
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                plugin.getMessages().getString("GENERAL.ToggleFailure"));
            return;
        }
        if (newDoor.isLocked())
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                plugin.getMessages().getString("GENERAL.DoorIsLocked"));

        else
        {
            Opener opener = plugin.getDoorOpener(newDoor.getType());
            DoorOpenResult result = opener == null ? DoorOpenResult.TYPEDISABLED : opener.openDoor(newDoor, time);

            if (result != DoorOpenResult.SUCCESS)
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                    plugin.getMessages().getString(DoorOpenResult.getMessage(result)));
        }
    }

    public double parseDoorsAndTime(CommandSender sender, String[] args, ArrayList<Door> doors)
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
        while (index-- > 0)
        {
            Door door = plugin.getCommander().getDoor(args[index], player);
            if (door == null)
            {
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[index] + "\" "
                    + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
                continue;
            }
            doors.add(door);
        }
        return time;
    }

    public void toggleDoorCommand(Player player, Door door, double time)
    {
        toggleDoorCommand((CommandSender) player, door, time);
    }

    public void toggleDoorCommand(Player player, Door door)
    {
        toggleDoorCommand((CommandSender) player, door, 0.0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        ArrayList<Door> doors = new ArrayList<>();
        double time = parseDoorsAndTime(sender, args, doors);

        for (Door door : doors)
            toggleDoorCommand(sender, door, time);
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
    public String getPermission()
    {
        return permission;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }
}
