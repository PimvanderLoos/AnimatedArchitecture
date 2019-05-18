package nl.pim16aap2.bigDoors.handlers;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.GUI.GUI;
import nl.pim16aap2.bigDoors.moveBlocks.BlockMover;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.toolUsers.DoorCreator;
import nl.pim16aap2.bigDoors.toolUsers.DrawbridgeCreator;
import nl.pim16aap2.bigDoors.toolUsers.ElevatorCreator;
import nl.pim16aap2.bigDoors.toolUsers.FlagCreator;
import nl.pim16aap2.bigDoors.toolUsers.PortcullisCreator;
import nl.pim16aap2.bigDoors.toolUsers.PowerBlockInspector;
import nl.pim16aap2.bigDoors.toolUsers.PowerBlockRelocator;
import nl.pim16aap2.bigDoors.toolUsers.SlidingDoorCreator;
import nl.pim16aap2.bigDoors.toolUsers.ToolUser;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.DoorAttribute;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.XMaterial;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForAddOwner;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForCommand;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForRemoveOwner;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForSetBlocksToMove;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForSetTime;

public class CommandHandler implements CommandExecutor
{
    BigDoors plugin;

    public CommandHandler(BigDoors plugin)
    {
        this.plugin = plugin;
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
        if (sender instanceof Player &&
            !plugin.getCommander().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.TOGGLE))
            return;

        UUID playerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        // Get a new instance of the door to make sure the locked / unlocked status is recent.
        Door newDoor = plugin.getCommander().getDoor(playerUUID, door.getDoorUID());

        if (newDoor == null)
        {
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, plugin.getMessages().getString("GENERAL.ToggleFailure"));
            return;
        }
        if (newDoor.isLocked())
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, plugin.getMessages().getString("GENERAL.DoorIsLocked"));

        else
        {
            Opener opener = plugin.getDoorOpener(newDoor.getType());
            DoorOpenResult result = opener == null ? DoorOpenResult.TYPEDISABLED : opener.openDoor(newDoor, time);

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
            Util.messagePlayer(player, Util.getFullDoorInfo(plugin.getCommander().getDoor(player.getUniqueId(), doorUID)));
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
        if (plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.INFO))
            Util.messagePlayer(player, Util.getFullDoorInfo(door));
    }

    public void listDoorInfoFromConsole(String str)
    {
        long doorUID = Util.longFromString(str, -1L);
        if (doorUID == -1)
        {
            ArrayList<Door> doors = plugin.getCommander().getDoors(null, str);
            for (Door door : doors)
                plugin.getMyLogger().myLogger(Level.INFO, plugin.getCommander().playerNameFromUUID(door.getPlayerUUID()) + ": " + Util.getFullDoorInfo(door));
            if (doors.size() == 0)
                plugin.getMyLogger().myLogger(Level.INFO, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            return;
        }
        Door door = plugin.getCommander().getDoor(null, doorUID);
        if (door == null)
        {
            plugin.getMyLogger().myLogger(Level.INFO, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            return;
        }
        plugin.getMyLogger().myLogger(Level.INFO, Util.nameFromUUID(door.getPlayerUUID()) + ": " + Util.getFullDoorInfo(door));
    }

    private void listDoorsFromConsole(String playerUUID, String name)
    {
        ArrayList<Door> doors = plugin.getCommander().getDoors(playerUUID, name);
        for (Door door : doors)
        {
            String playerName = plugin.getCommander().playerNameFromUUID(UUID.fromString(playerUUID));
            plugin.getMyLogger().myLogger(Level.INFO,
                                          (playerName == null ? "" : playerName + ": ") + Util.getBasicDoorInfo(door));
        }
        if (doors.size() == 0)
            plugin.getMyLogger().myLogger(Level.INFO, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
    }

    public void listDoorsFromConsole(String str)
    {
        UUID playerUUID = plugin.getCommander().playerUUIDFromName(str);
        if (playerUUID != null)
            listDoorsFromConsole(playerUUID.toString(), null);
        else
            plugin.getMyLogger().info("Player \"" + str + "\" was not found!");
    }

    // Doors aren't allowed to have numerical names, to differentiate doorNames from doorUIDs.
    public boolean isValidDoorName(String name)
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
        if (!player.hasPermission(DoorType.getPermission(type)))
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

        if (name != null && !isValidDoorName(name))
        {
            Util.messagePlayer(player, ChatColor.RED, "\"" + name + "\"" + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
            return;
        }

        if (isPlayerBusy(player))
            return;

        // These are disabled.
        if (type.equals(DoorType.FLAG) || type.equals(DoorType.ELEVATOR)) // DISABLED ELEVATORS
            return;

        ToolUser tu = type == DoorType.DOOR        ? new DoorCreator       (plugin, player, name) :
                      type == DoorType.DRAWBRIDGE  ? new DrawbridgeCreator (plugin, player, name) :
                      type == DoorType.PORTCULLIS  ? new PortcullisCreator (plugin, player, name) :
                      type == DoorType.ELEVATOR    ? new ElevatorCreator   (plugin, player, name) :
                      type == DoorType.FLAG        ? new FlagCreator       (plugin, player, name) :
                      type == DoorType.SLIDINGDOOR ? new SlidingDoorCreator(plugin, player, name) : null;

        startTimerForAbortable(tu, 60 * 20);
    }

    public void startTimerForAbortable(Abortable abortable, int time)
    {
        BukkitTask task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                abortable.abort(false);
            }
        }.runTaskLater(plugin, time);
        abortable.setTask(task);
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
        if (plugin.getCommander().hasPermissionForAction(player, doorUID, DoorAttribute.CHANGETIMER))
            plugin.getCommander().updateDoorAutoClose(doorUID, autoClose);
    }

    public void setDoorBlocksToMove(Player player, long doorUID, int autoClose)
    {
        if (plugin.getCommander().hasPermissionForAction(player, doorUID, DoorAttribute.BLOCKSTOMOVE))
            plugin.getCommander().updateDoorBlocksToMove(doorUID, autoClose);
    }

    public void startBlocksToMoveSetter(Player player, long doorUID)
    {
        if (isPlayerBusy(player))
            return;
        startTimerForAbortable((new WaitForSetBlocksToMove(plugin, player, doorUID)), 20 * 20);
    }

    private void replaceWaitForCommand(Player player)
    {
        WaitForCommand cw = plugin.getCommandWaiter(player);
        if (cw != null)
        {
            cw.setFinished(true); // I don't want to print any timeout messages, as it's being replaced.
            abortAbortable(cw);
        }
    }

    public void startTimerSetter(Player player, long doorUID)
    {
        replaceWaitForCommand(player);
        startTimerForAbortable((new WaitForSetTime(plugin, player, doorUID)), 20 * 20);
    }

    public void startAddOwner(Player player, long doorUID)
    {
        replaceWaitForCommand(player);
        startTimerForAbortable((new WaitForAddOwner(plugin, player, doorUID)), 20 * 20);
    }

    public void startRemoveOwner(Player player, long doorUID)
    {
        replaceWaitForCommand(player);
        startTimerForAbortable((new WaitForRemoveOwner(plugin, player, doorUID)), 20 * 20);
    }

    private boolean isPlayerBusy(Player player)
    {
        boolean isBusy = (plugin.getToolUser(player) != null || isCommandWaiter(player) != null);
        if (isBusy)
            Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.IsBusy"));
        return isBusy;
    }

    private void abortAbortable(Abortable abortable)
    {
        if (abortable instanceof ToolUser)
            ((ToolUser) abortable).setIsDone(true);
        abortable.abort();
    }

    public void startPowerBlockRelocator(Player player, long doorUID)
    {
        startTimerForAbortable(new PowerBlockRelocator(plugin, player, doorUID), 20 * 20);
    }

    // Handle commands.
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player player = sender instanceof Player ? (Player) sender : null;

        if (cmd.getName().equalsIgnoreCase("bigdoors"))
        {
            String firstCommand = args.length == 0 ? "" : args[0].toLowerCase();

            switch(firstCommand)
            {
            case "version":
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "This server uses version " +
                    plugin.getDescription().getVersion() + " of this plugin!");
                break;

            case "menu":
                if (player != null)
                    plugin.addGUIUser(new GUI(plugin, player));
                break;

            case "restart":
                plugin.restart();
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "BigDoors has been restarted!");
                break;

            case "stop":
                if ((player != null && player.hasPermission("bigdoors.admin.stopdoors")) || player == null)
                    stopDoors();
                break;

            case "pause":
                if ((player != null && player.hasPermission("bigdoors.admin.pausedoors")) || player == null)
                    plugin.getCommander().togglePaused();
                break;

            case "addowner":
                if (player == null) // TODO: Surely, the server should be able to add any user to any door?
                    break;

                if (args.length > 1)
                {
                    WaitForCommand cw = isCommandWaiter(player);
                    if (cw != null)
                        return cw.executeCommand(args);

                    Door door = plugin.getCommander().getDoor(args[1], player);
                    if (door == null)
                    {
                        plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                            ChatColor.RED, "\"" + args[1] + "\" " + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
                        return true;
                    }

                    UUID playerUUID = null;
                    int permission = 1;
                    try
                    {
                        playerUUID = plugin.getCommander().playerUUIDFromName(args[2]);
                        if (args.length == 4)
                            permission = Integer.parseInt(args[3]);
                    }
                    catch (Exception uncaught)
                    {
                        // No need to catch this. I don't care if people fuck it up.
                    }
                    if (playerUUID != null)
                    {
                        if (plugin.getCommander().addOwner(playerUUID, door, permission))
                        {
                            plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                                ChatColor.RED, plugin.getMessages().getString("COMMAND.AddOwner.Success"));
                            return true;
                        }
                        plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                            ChatColor.RED, plugin.getMessages().getString("COMMAND.AddOwner.Fail"));
                        return false;
                    }
                    plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                        ChatColor.RED, plugin.getMessages().getString("GENERAL.PlayerNotFound") + ": \"" + args[2] + "\"");
                    return true;
                }
                // TODO: Print help menu!
                Util.broadcastMessage("FAIL");
                return true;

            case "removeowner":
                if (player == null)
                    break;

                if (args.length == 2)
                {
                    WaitForCommand cw = isCommandWaiter(player);
                    if (cw != null)
                        return cw.executeCommand(args);
                }
                else if (args.length > 2)
                {
                    // If the player is currently in a commandWaiter, just abort that and use the direct one instead.
                    WaitForCommand cw = isCommandWaiter(player);
                    if (cw != null && cw.getCommand().equals("removeowner"))
                        cw.abortSilently();

                    Door door = plugin.getCommander().getDoor(args[1], player);
                    if (door == null)
                    {
                        plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                            ChatColor.RED, "\"" + args[1] + "\" " + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
                        return true;
                    }

                    UUID playerUUID = Util.playerUUIDFromString(args[2]);

                    if (playerUUID != null)
                    {
                        if (plugin.getCommander().removeOwner(door, playerUUID))
                        {
                            plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                                ChatColor.RED, plugin.getMessages().getString("COMMAND.RemoveOwner.Success"));
                            return true;
                        }
                        plugin.getMyLogger().returnToSender(sender, Level.INFO,
                                                            ChatColor.RED, plugin.getMessages().getString("COMMAND.RemoveOwner.Fail"));
                        return false;
                    }
                }
                break;

            case "":
            case "help":
            default:
                showHelpInfo(sender);
            }
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
            doorDebug(player);
            return true;
        }

        // /setautoclosetime <doorName> <time>
        if (cmd.getName().equalsIgnoreCase("setautoclosetime"))
        {
            // If there is only 1 argument, assume that it is a player using the commandWaiter system
            // And therefore the Door is already defined somewhere else (e.g. selected from the GUI).
            if (args.length == 1 && player != null)
            {
                WaitForCommand cw = isCommandWaiter(player);
                if (cw != null)
                    return cw.executeCommand(args);
            }
            else if (args.length == 2)
            {
                // If the player is currently in a commandWaiter, just abort that and use the direct one instead.
                WaitForCommand cw = isCommandWaiter(player);
                if (cw != null && cw.getCommand().equals("setautoclosetime"))
                    cw.abortSilently();

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

        // /setblockstomove <doorName> <distance>
        if (cmd.getName().equalsIgnoreCase("setblockstomove"))
        {
            // If there is only 1 argument, assume that it is a player using the commandWaiter system
            // And therefore the Door is already defined somewhere else (e.g. selected from the GUI).
            if (args.length == 1 && player != null)
            {
                WaitForCommand cw = isCommandWaiter(player);
                if (cw != null)
                    return cw.executeCommand(args);
            }
            else if (args.length == 2)
            {
                // If the player is currently in a commandWaiter, just abort that and use the direct one instead.
                WaitForCommand cw = isCommandWaiter(player);
                if (cw != null && cw.getCommand().equals("setblockstomove"))
                    cw.abortSilently();

                Door door = plugin.getCommander().getDoor(args[0], player);
                if (door == null)
                    return false;

                try
                {
                    int blocksToMove = Integer.parseInt(args[1]);
                    setDoorBlocksToMove(player, door.getDoorUID(), blocksToMove);
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
                            if (!door.isOpen())
                                openDoorCommand(sender, door, time);
                            else
                                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                                    plugin.getMessages().getString("GENERAL.Door") + " \"" + args[index] +
                                                                    "\" " + plugin.getMessages().getString("GENERAL.DoorAlreadyOpen"));
                }
                return true;
            }
        }

        // /listdoors [name]
        // /listdoors <doorName || playerName>
        if (cmd.getName().equalsIgnoreCase("listdoors"))
        {
            if (player != null)
            {
                if (args.length == 0)
                    listDoors(player, null);
                else if (args.length == 1)
                    listDoors(player, args[0]);
            }
            else if (args.length == 0)
                return false;
            else if (args.length == 1)
                listDoorsFromConsole(args[0]);

            return true;
        }

        // /doorinfo <doorName>
        // /doorinfo <doorUID>
        if (cmd.getName().equalsIgnoreCase("doorinfo"))
            if (args.length == 1)
            {
                if (player != null)
                    listDoorInfo(player, args[0]);
                else
                    listDoorInfoFromConsole(args[0]);
                return true;
            }

        if (player != null)
        {
            // /inspectpowerblockloc
            if (cmd.getName().equalsIgnoreCase("inspectpowerblockloc"))
            {
                if (isPlayerBusy(player))
                    return false;
                startTimerForAbortable(new PowerBlockInspector(plugin, player, -1), 20 * 20);
                return true;
            }

            // /bdm
            if (cmd.getName().equalsIgnoreCase("bdm"))
            {
                plugin.addGUIUser(new GUI(plugin, player));
                return true;
            }

            // /namedoor
            if (cmd.getName().equalsIgnoreCase("namedoor"))
            {
                ToolUser tu = plugin.getToolUser(player);
                if (tu != null)
                {
                    if (args.length == 1)
                        if (isValidDoorName(args[0]))
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
                ToolUser tu = plugin.getToolUser(player);
                if (tu != null)
                {
                    abortAbortable(tu);
                    plugin.getMyLogger().returnToSender(player, Level.INFO, ChatColor.RED, plugin.getMessages().getString("CREATOR.GENERAL.Cancelled"));
                }
                else
                {
                    WaitForCommand cw = plugin.getCommandWaiter(player);
                    if (cw != null)
                        abortAbortable(cw);
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
                        type = DoorType.valueOfFlag(args[0].toUpperCase());
                        if (type == null)
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
        Util.broadcastMessage("Loaded chunks: " + player.getLocation().getWorld().getLoadedChunks().length);
        for (BlockMover bm : plugin.getBlockMovers())
        {
            Chunk chunk = bm.getDoor().getPowerBlockLoc().getChunk();
            Util.broadcastMessage("Active chunk: " + chunk.getX() + ":" + chunk.getZ());
        }


//        Door d1 = plugin.getCommander().getDoor (player.getUniqueId(), 10);
//        Door d2 = plugin.getCommander().getDoor2(player.getUniqueId(), 10);

//        long startTime = System.nanoTime();
//
//        for (int idx = 0; idx < 100000; ++idx)
//            plugin.getCommander().getDoor(player.getUniqueId(), 10);
//
//        long endTime = System.nanoTime();
//        long timeElapsed = endTime - startTime;
//        Util.broadcastMessage("Method 1 took " + timeElapsed);
//
//
//
//        startTime = System.nanoTime();
//
//
//        endTime = System.nanoTime();
//        timeElapsed = endTime - startTime;
//        Util.broadcastMessage("Method 2 took " + timeElapsed);

//        Util.broadcastMessage("Going to display cache stats now!");
//        long doorCount = 0;
//
//        ArrayList<Long> chunks = plugin.getPBCache().getKeys();
//        if (chunks == null)
//            Util.broadcastMessage("No chunks in cache!");
//        else
//        {
//            for (Long chunkHash : chunks)
//                doorCount += plugin.getPBCache().get(chunkHash).size();
//
//            Util.broadcastMessage("# of doors in cache: " + doorCount + " in " + plugin.getPBCache().getChunkCount() + " chunks.");
//
//            String doorsStr = "";
//
//            for (Long chunkHash : chunks)
//            {
//                HashMap<Long, Long> doors = plugin.getPBCache().get(chunkHash);
//                Iterator<Entry<Long, Long>> it = doors.entrySet().iterator();
//                while (it.hasNext())
//                {
//                    Entry<Long, Long> entry = it.next();
//                    Door door = plugin.getCommander().getDoor(player.getUniqueId(), entry.getValue());
//                    doorsStr += entry.getValue() + " (" + door.getPowerBlockLoc() + " = " + door.getPowerBlockChunkHash() + "), ";
//                }
//            }
//            Util.broadcastMessage("Doors found: " + doorsStr);
//        }
    }

    private String helpFormat(String command, String explanation)
    {
        String help = "";
        help += String.format(ChatColor.GREEN + "%s " + ChatColor.BLUE + "%s\n", command, explanation);
        return help;
    }

    private void showHelpInfo(CommandSender sender)
    {
        Player player = sender instanceof Player ? (Player) sender : null;
        String help = "";
        help += ChatColor.GREEN + "====[ BigDoors Help ]====\n";
        help += helpFormat("/BigDoors menu", "Opens BigDoors' GUI.");
        help += helpFormat("/BigDoors version", "Get the version of this plugin.");
        help += helpFormat("/BigDoors removeowner <door> <player>", "Add another owner for a door.");
        help += helpFormat("/BigDoors addowner <door> <player> [permission]", "Add another owner for a door.");
        if (player == null || player.hasPermission("bigdoors.admin.restart"))
            help += helpFormat("/BigDoors restart", "Restart the plugin. Reinitializes almost everything.");
        if (player == null || player.hasPermission("bigdoors.admin.stopdoors"))
            help += helpFormat("/BigDoors stop", "Forces all doors to finish instantly.");
        if (player == null || player.hasPermission("bigdoors.admin.pausedoors"))
            help += helpFormat("/BigDoors pause", "Pauses all door movement until the command is run again.");
        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, help);
    }
}
