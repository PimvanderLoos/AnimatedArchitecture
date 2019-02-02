package nl.pim16aap2.bigDoors.handlers;

import java.util.ArrayList;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.GUI.GUIPage;
import nl.pim16aap2.bigDoors.toolUsers.DoorCreator;
import nl.pim16aap2.bigDoors.toolUsers.DrawbridgeCreator;
import nl.pim16aap2.bigDoors.toolUsers.PortcullisCreator;
import nl.pim16aap2.bigDoors.toolUsers.PowerBlockInspector;
import nl.pim16aap2.bigDoors.toolUsers.PowerBlockRelocator;
import nl.pim16aap2.bigDoors.toolUsers.ToolUser;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.XMaterial;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForCommand;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForSetTime;

public class CommandHandler implements CommandExecutor
{
    BigDoors plugin;

    public CommandHandler(BigDoors plugin)
    {
        this.plugin   = plugin;
    }

    public void stopDoors()
    {
        plugin.getCommander().setCanGo(false);
        plugin.getCommander().emptyBusyDoors();
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                plugin.getCommander().setCanGo(true);
            }
        }.runTaskLater(plugin, 5L);
    }

    // Open the door.
    public void openDoorCommand(CommandSender sender, Door door, double time)
    {
        // Get a new instance of the door to make sure the locked / unlocked status is recent.
        if (plugin.getCommander().getDoor(door.getDoorUID()).isLocked())
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, plugin.getMessages().getString("GENERAL.DoorIsLocked"));
        // If the sender is a Player && the player's permission level is larger than 1 for this door, the player doesn't have permission (for now).
        else if (sender instanceof Player && plugin.getCommander().getPermission(((Player) (sender)).getUniqueId().toString(), door.getDoorUID()) > 1)
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, plugin.getMessages().getString("GENERAL.NoPermissionToOpen"));
//        else if (!plugin.getDoorOpener(door.getType()).openDoor(door, time))
//            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, plugin.getMessages().getString("GENERAL.ToggleFailure"));
        else
        {
            DoorOpenResult result = plugin.getDoorOpener(door.getType()).openDoor(door, time);
            if (result != DoorOpenResult.SUCCESS)
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, plugin.getMessages().getString(DoorOpenResult.getMessage(result)));
        }
    }

    public void openDoorCommand(Player player, Door door, double time)
    {
        openDoorCommand((CommandSender) player, door, time);
    }

    public void openDoorCommand(Player player, Door door)
    {
        openDoorCommand((CommandSender) player, door, 0.0);
    }

    public void lockDoorCommand(Player player, Door door)
    {
        plugin.getCommander().setLock(door.getDoorUID(), !door.isLocked());
    }

    // Get the number of doors owned by player player with name doorName (or any name, if doorName == null)
    public long countDoors(Player player, String doorName)
    {
        return plugin.getCommander().countDoors(player.getUniqueId().toString(), doorName);
    }

    // Print a list of the doors currently in the db.
    public void listDoors(Player player, String doorName)
    {
        ArrayList<Door> doors = plugin.getCommander().getDoors(player.getUniqueId().toString(), doorName);
        for (Door door : doors)
            Util.messagePlayer(player, Util.getBasicDoorInfo(door));
        if (doors.size() == 0)
            Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
    }

    // Print a list of the doors currently in the db.
    public void listDoorInfo(Player player, String name)
    {
        long doorUID = Util.longFromString(name, -1L);
        if (doorUID != -1)
            Util.messagePlayer(player, Util.getFullDoorInfo(plugin.getCommander().getDoor(doorUID)));
        else
        {
            ArrayList<Door> doors = plugin.getCommander().getDoors(player.getUniqueId().toString(), name);
            for (Door door : doors)
                Util.messagePlayer(player, Util.getFullDoorInfo(door));
            if (doors.size() == 0)
                Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
        }
    }

    public void listDoorInfo(Player player, Door door)
    {
        Util.messagePlayer(player, Util.getFullDoorInfo(door));
    }

    public void listDoorInfoFromConsole(String str)
    {
        long doorUID = Util.longFromString(str, -1L);
        if (doorUID == -1)
        {
            ArrayList<Door> doors = plugin.getCommander().getDoors(null, str);
            for (Door door : doors)
                plugin.getMyLogger().myLogger(Level.INFO, Util.nameFromUUID(door.getPlayerUUID()) + ": " + Util.getFullDoorInfo(door));
            if (doors.size() == 0)
                plugin.getMyLogger().myLogger(Level.INFO, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            return;
        }
        Door door = plugin.getCommander().getDoor(doorUID);
        if (door == null)
        {
            plugin.getMyLogger().myLogger(Level.INFO, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            return;
        }
        plugin.getMyLogger().myLogger(Level.INFO, Util.nameFromUUID(door.getPlayerUUID()) + ": " + Util.getFullDoorInfo(door));
    }

    private void listDoorsFromConsole(String playerUID, String name)
    {
        ArrayList<Door> doors = plugin.getCommander().getDoors(playerUID, name);
        for (Door door : doors)
        {
            String playerName = Util.nameFromUUID(door.getPlayerUUID());
            plugin.getMyLogger().myLogger(Level.INFO,
                                          (playerName == null ? "" : playerName + ": ") + Util.getBasicDoorInfo(door));
        }
        if (doors.size() == 0)
            plugin.getMyLogger().myLogger(Level.INFO, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
    }

    public void listDoorsFromConsole(String str)
    {
        String playerUUID = Util.playerUUIDFromString(str);
        if (playerUUID != null)
            listDoorsFromConsole(playerUUID, null);
        else
            listDoorsFromConsole(null, str);
    }

    public boolean isValidName(String name)
    {
        try
        {
            Integer.parseInt(name);
            return false;
        }
        catch(NumberFormatException e)
        {
            return true;
        }
    }

    // Create a new door.
    public void startCreator(Player player, String name, DoorType type)
    {
    	if (type == DoorType.DOOR       && !player.hasPermission("bigdoors.user.createdoor.door")       ||
			type == DoorType.DRAWBRIDGE && !player.hasPermission("bigdoors.user.createdoor.drawbridge") ||
			type == DoorType.PORTCULLIS && !player.hasPermission("bigdoors.user.createdoor.portcullis"))
    	{
            Util.messagePlayer(player, ChatColor.RED, plugin.getMessages().getString("GENERAL.NoDoorTypeCreationPermission"));
            return;
    	}

        long doorCount = plugin.getCommander().countDoors(player.getUniqueId().toString(), null);
        int maxCount   = Util.getMaxDoorsForPlayer(player);
        if (maxCount  >= 0 && doorCount >= maxCount)
        {
            Util.messagePlayer(player, ChatColor.RED, plugin.getMessages().getString("GENERAL.TooManyDoors"));
            return;
        }

        if (name != null && !isValidName(name))
        {
            Util.messagePlayer(player, ChatColor.RED, "\"" + name + "\"" + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
            return;
        }

        if (isPlayerBusy(player))
            return;

        ToolUser tu = type == DoorType.DOOR       ? new DoorCreator      (plugin, player, name) :
                      type == DoorType.DRAWBRIDGE ? new DrawbridgeCreator(plugin, player, name) :
                      type == DoorType.PORTCULLIS ? new PortcullisCreator(plugin, player, name) : null;

        startTimerForAbortable(tu, player, 60 * 20);
    }

    public ToolUser isToolUser(Player player)
    {
        for (ToolUser tu : plugin.getToolUsers())
            if (tu.getPlayer() == player)
                return tu;
        return null;
    }

    public void startTimerForAbortable(Abortable abortable, Player player, int time)
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                abortable.abort(false);
            }
        }.runTaskLater(plugin, time);
    }

    public WaitForCommand isCommandWaiter(Player player)
    {
        for (WaitForCommand cw : plugin.getCommandWaiters())
            if (cw.getPlayer() == player)
                return cw;
        return null;
    }

    public void setDoorOpenTime(Player player, long doorUID, int autoClose)
    {
        plugin.getCommander().updateDoorAutoClose(doorUID, autoClose);
    }

    public void startTimerSetter(Player player, long doorUID)
    {
        if (isPlayerBusy(player))
            return;
        startTimerForAbortable((new WaitForSetTime(plugin, player, "setautoclosetime", doorUID)), player, 20 * 20);
    }

    private boolean isPlayerBusy(Player player)
    {
        boolean isBusy = (isToolUser(player) != null || isCommandWaiter(player) != null);
        if (isBusy)
            Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.IsBusy"));
        return isBusy;
    }

    public void startPowerBlockRelocator(Player player, long doorUID)
    {
        startTimerForAbortable(new PowerBlockRelocator(plugin, player, doorUID), player, 20 * 20);
    }

    // Handle commands.
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player player;

        // /bdrestart
        if (cmd.getName().equalsIgnoreCase("bdrestart"))
        {
            plugin.restart();
            return true;
        }

        // /stopdoors
        if (cmd.getName().equalsIgnoreCase("stopdoors"))
        {
            stopDoors();
            return true;
        }

        // /doordebug
        if (cmd.getName().equalsIgnoreCase("doordebug"))
        {
            player = null;
            if (sender instanceof Player)
                player = (Player) sender;
            doorDebug(player);
            return true;
        }

        // /setautoclosetime <doorName> <time>
        if (cmd.getName().equalsIgnoreCase("setautoclosetime"))
        {
            player = null;
            if (sender instanceof Player)
                player = (Player) sender;

            if (args.length == 1 && player != null)
            {
                WaitForCommand cw = isCommandWaiter(player);
                if (cw != null)
                    return cw.executeCommand(args);
            }
            else if (args.length == 2)
            {
                Door door = plugin.getCommander().getDoor(args[0], player);
                if (door == null)
                    return false;

                try
                {
                    int time = Integer.parseInt(args[1]);
                    setDoorOpenTime(player, door.getDoorUID(), time);
                    return true;
                }
                catch (Exception e)
                {
                    return false;
                }
            }
            return false;
        }

        // /setdoorrotation <doorName> <CLOCK || COUNTER || ANY>
        if (cmd.getName().equalsIgnoreCase("setdoorrotation"))
        {
            if (args.length != 2)
                return false;
            player = null;
            if (sender instanceof Player)
                player = (Player) sender;
            Door door = plugin.getCommander().getDoor(args[0], player);
            if (door == null)
                return false;

            RotateDirection openDir = null;
            if (args[1].equalsIgnoreCase("CLOCK") || args[1].equalsIgnoreCase("CLOCKWISE"))
                openDir = RotateDirection.CLOCKWISE;
            else if (args[1].equalsIgnoreCase("COUNTER") || args[1].equalsIgnoreCase("COUNTERCLOCKWISE"))
                openDir = RotateDirection.COUNTERCLOCKWISE;
            else if (args[1].equalsIgnoreCase("NONE") || args[1].equalsIgnoreCase("ANY"))
                openDir = RotateDirection.NONE;
            else
                return false;

            plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), openDir);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("filldoor"))
        {
            if (args.length != 1)
                return false;
            player = null;
            if (sender instanceof Player)
                player = (Player) sender;
            Door door = plugin.getCommander().getDoor(args[0], player);
            if (door == null)
                return false;

            for (int i = door.getMinimum().getBlockX(); i <= door.getMaximum().getBlockX(); ++i)
                for (int j = door.getMinimum().getBlockY(); j <= door.getMaximum().getBlockY(); ++j)
                    for (int k = door.getMinimum().getBlockZ(); k <= door.getMaximum().getBlockZ(); ++k)
                        door.getWorld().getBlockAt(i, j, k).setType(XMaterial.STONE.parseMaterial());
            return true;
        }

        // /pausedoors
        if (cmd.getName().equalsIgnoreCase("pausedoors"))
        {
            plugin.getCommander().togglePaused();
            return true;
        }

        // /bdversion
        if (cmd.getName().equalsIgnoreCase("bdversion"))
        {
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "This server uses version " +
                    plugin.getDescription().getVersion() + " of this plugin!");
            return true;
        }

        // /opendoor <doorName 1> [doorName 2] ... [doorName x] [time]
        if (cmd.getName().equalsIgnoreCase("opendoor") ||
                cmd.getName().equalsIgnoreCase("closedoor") ||
                cmd.getName().equalsIgnoreCase("toggledoor"))
        {
            // type0 = opendoor, type1 = closedoor, type2 = toggledoor
            int type = cmd.getName().equalsIgnoreCase("opendoor")  ? 0 :
                cmd.getName().equalsIgnoreCase("closedoor") ? 1 : 2;

            if (args.length >= 1)
            {
                if (sender instanceof Player)
                    player = (Player) sender;
                else
                    player = null;

                String lastStr = args[args.length - 1];
                // Last argument sets speed if it's a double.
                double time = Util.longFromString(lastStr, -1L) == -1L ? Util.doubleFromString(lastStr, 0.0D) : 0.0D;
                int endIDX = args.length;
                // If the time variable was specified, decrement endIDX by 1, as the last argument is not a door!
                if (time != 0.0D)
                    --endIDX;
                // Go over all arguments until the last one (or the one before that).
                for (int index = 0; index < endIDX; ++index)
                {
                    Door door = plugin.getCommander().getDoor(args[index], player);
                    // If the door is null, let the player know that the selected door is invalid.
                    // TODO: Would you look at that? But seriously, this seems to just check if the last argument
                    // Is a valid door, but if it was the time, it shouldn't get here anyway!
                    if (door == null && index != args.length - 1)
                    {
                        plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                            ChatColor.RED, "\"" + args[index] + "\" " + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
                        // If player is null, the console or a command block is being used.
                        // If that's the case, kindly remind them (in the console) that they should be using DoorUIDs.
                        if (player == null)
                            plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                                ChatColor.RED, "Don't forget that you should use the DoorUID in the console/command blocks! DoorName won't work here!");
                    }
                    else if (door != null)
                    {
                        if (type == 2 || door.getOpenDir().equals(RotateDirection.NONE))
                            openDoorCommand(sender, door, time);
                        else if (type == 1)
                        {
                            if (door.isOpen())
                                openDoorCommand(sender, door, time);
                            else
                                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                                    plugin.getMessages().getString("GENERAL.Door") + " \"" + args[index] +
                                                                    "\" " + plugin.getMessages().getString("GENERAL.DoorAlreadyClosed"));
                        }
                        else if (type == 0)
                        {
                            if (!door.isOpen())
                                openDoorCommand(sender, door, time);
                            else
                                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                                    plugin.getMessages().getString("GENERAL.Door") + " \"" + args[index] +
                                                                    "\" " + plugin.getMessages().getString("GENERAL.DoorAlreadyOpen"));
                        }
                    }
                }
                return true;
            }
        }

        // /listdoors [name]
        // /listdoors <doorName || playerName>
        if (cmd.getName().equalsIgnoreCase("listdoors"))
        {
            if (sender instanceof Player)
            {
                player = (Player) sender;
                if (args.length == 0)
                    listDoors(player, null);
                else if (args.length == 1)
                    listDoors(player, args[0]);
            }
            else
            {
                if (args.length == 0)
                    return false;
                else if (args.length == 1)
                    listDoorsFromConsole(args[0]);
            }

            return true;
        }

        // /doorinfo <doorName>
        // /doorinfo <doorUID>
        if (cmd.getName().equalsIgnoreCase("doorinfo"))
        {
            if (args.length == 1)
            {
                if (sender instanceof Player)
                {
                    player = (Player) sender;
                    listDoorInfo(player, args[0]);
                }
                else
                {
                    listDoorInfoFromConsole(args[0]);
                }
                return true;
            }
        }

        if (sender instanceof Player)
        {
            player = (Player) sender;

            // /bigdoorsenableas
            if (cmd.getName().equalsIgnoreCase("bigdoorsenableas"))
            {
                plugin.bigDoorsEnableAS();
                return true;
            }

            // /inspectpowerblockloc
            if (cmd.getName().equalsIgnoreCase("inspectpowerblockloc"))
            {
                if (isPlayerBusy(player))
                    return false;
                startTimerForAbortable(new PowerBlockInspector(plugin, player, -1), player, 20 * 20);
                return true;
            }

            // /bigdoors
            if (cmd.getName().equalsIgnoreCase("bigdoors") || cmd.getName().equalsIgnoreCase("bdm"))
            {
                new GUIPage(plugin, player);
                return true;
            }

            // /namedoor
            if (cmd.getName().equalsIgnoreCase("namedoor"))
            {
                ToolUser tu = isToolUser(player);
                if (tu != null)
                {
                    if (args.length == 1)
                        if (isValidName(args[0]))
                        {
                            tu.setName(args[0]);
                            return true;
                        }
                }
                else
                    Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.NotBusy"));
            }

            // /changePowerBlockLoc
            if (cmd.getName().equalsIgnoreCase("changePowerBlockLoc"))
            {
                if (args.length < 1)
                    return false;
                Door door = plugin.getCommander().getDoor(args[0], player);
                if (door == null)
                    return true;
                long doorUID = door.getDoorUID();
                startPowerBlockRelocator(player, doorUID);
                return true;
            }

            // /bdcancel
            if (cmd.getName().equalsIgnoreCase("bdcancel"))
            {
                ToolUser tu = isToolUser(player);
                if (tu != null)
                {
                    tu.setIsDone(true);
                    plugin.getMyLogger().returnToSender(player, Level.INFO, ChatColor.RED, plugin.getMessages().getString("CREATOR.GENERAL.Cancelled"));
                }
                return true;
            }

            // deldoor <doorName>
            if (cmd.getName().equalsIgnoreCase("deldoor"))
                if (args.length == 1)
                {
                    delDoor(player, args[0]);
                    return true;
                }

            // /newdoor <doorName>
            if (cmd.getName().equalsIgnoreCase("newdoor"))
                if (args.length >= 1)
                {
                    DoorType type = DoorType.DOOR;
                    String name;
                    if (args.length == 2)
                    {
                        if      (args[0].equalsIgnoreCase("-pc"))
                            type = DoorType.PORTCULLIS;
                        else if (args[0].equalsIgnoreCase("-db"))
                            type = DoorType.DRAWBRIDGE;
                        else if (args[0].equalsIgnoreCase("-bd"))
                            type = DoorType.DOOR;
                        else
                            return false;
                        name = args[1];
                    }
                    else
                        name = args[0];

                    startCreator(player, name, type);
                    return true;
                }

            // /newportcullis <doorName>
            if (cmd.getName().equalsIgnoreCase("newportcullis"))
                if (args.length == 1)
                {
                    startCreator(player, args[0], DoorType.PORTCULLIS);
                    return true;
                }

            // /newdrawbridge <doorName>
            if (cmd.getName().equalsIgnoreCase("newdrawbridge"))
                if (args.length == 1)
                {
                    startCreator(player, args[0], DoorType.DRAWBRIDGE);
                    return true;
                }
        }
        return false;
    }

    public void delDoor(Player player, long doorUID)
    {
        plugin.getCommander().removeDoor(doorUID);
    }

    public void delDoor(Player player, String doorName)
    {
        CommandSender sender = player;
        try
        {
            long doorUID     = Long.parseLong(doorName);
            plugin.getCommander().removeDoor(doorUID);
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door deleted!");
            return;
        }
        catch(NumberFormatException e)
        {}

        long doorCount = countDoors(player, doorName);
        if (doorCount == 0)
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "No door found by that name!");
        else if (doorCount == 1)
        {
            plugin.getCommander().removeDoor(player.getUniqueId().toString(), doorName);
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door deleted!");
        }
        else
        {
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "More than one door found with that name! Please use their ID instead:");
            listDoors(player, doorName);
        }
    }

    // Used for various debugging purposes (you don't say!).
    public void doorDebug(Player player)
    {
        plugin.restart();
    }
}
