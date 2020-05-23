package nl.pim16aap2.bigDoors.handlers;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.GUI.GUI;
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
    public void openDoorCommand(CommandSender sender, Door door, double time, boolean instant)
    {
        if (sender instanceof Player &&
            !plugin.getCommander().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.TOGGLE))
            return;

        int globalLimit = BigDoors.get().getConfigLoader().maxDoorSize();
        int personalLimit = sender instanceof Player ? Util.getMaxDoorSizeForPlayer((Player) sender) : -1;
        int sizeLimit = Util.getLowestPositiveNumber(personalLimit, globalLimit);
        if (sizeLimit > 0 && sizeLimit <= door.getBlockCount())
        {
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                plugin.getMessages().getString("GENERAL.TooManyBlocks"));
            return;
        }

        Player player = sender instanceof Player ? (Player) sender : null;
        // Get a new instance of the door to make sure the locked / unlocked status is
        // recent.
        Door newDoor = plugin.getCommander().getDoor(Long.toString(door.getDoorUID()), player);

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
            DoorOpenResult result = opener == null ? DoorOpenResult.TYPEDISABLED :
                opener.openDoor(newDoor, time, instant);

            if (result != DoorOpenResult.SUCCESS)
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                    plugin.getMessages().getString(DoorOpenResult.getMessage(result)));
        }
    }

    public void openDoorCommand(Player player, Door door, double time)
    {
        openDoorCommand(player, door, time, false);
    }

    public void openDoorCommand(Player player, Door door)
    {
        openDoorCommand(player, door, 0.0, false);
    }

    // Get the number of doors owned by player player with name doorName (or any
    // name, if doorName == null)
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
        {
            if (plugin.getCommander().hasPermissionForAction(player, doorUID, DoorAttribute.INFO))
                Util.messagePlayer(player,
                                   Util.getFullDoorInfo(plugin.getCommander().getDoor(player.getUniqueId(), doorUID)));
        }
        else
        {
            ArrayList<Door> doors = plugin.getCommander().getDoors(player.getUniqueId().toString(), name);
            for (Door door : doors)
                if (plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.INFO))
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

    public void listDoorInfoIgnorePermission(Player player, Door door)
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
                plugin.getMyLogger().myLogger(Level.INFO, plugin.getCommander().playerNameFromUUID(door.getPlayerUUID())
                    + ": " + Util.getFullDoorInfo(door));
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
        plugin.getMyLogger().myLogger(Level.INFO,
                                      Util.nameFromUUID(door.getPlayerUUID()) + ": " + Util.getFullDoorInfo(door));
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

    // Doors aren't allowed to have numerical names, to differentiate doorNames from
    // doorUIDs.
    public boolean isValidDoorName(String name)
    {
        try
        {
            Integer.parseInt(name);
            return false;
        }
        catch (NumberFormatException e)
        {
            return true;
        }
    }

    // Create a new door.
    public void startCreator(Player player, String name, DoorType type)
    {
        if (!player.hasPermission(DoorType.getPermission(type)))
        {
            Util.messagePlayer(player, ChatColor.RED,
                               plugin.getMessages().getString("GENERAL.NoDoorTypeCreationPermission"));
            return;
        }

        long doorCount = plugin.getCommander().countDoors(player.getUniqueId().toString(), null);
        int maxCount = Util.getMaxDoorsForPlayer(player);
        if (maxCount >= 0 && doorCount >= maxCount)
        {
            Util.messagePlayer(player, ChatColor.RED, plugin.getMessages().getString("GENERAL.TooManyDoors"));
            return;
        }

        if (name != null && !isValidDoorName(name))
        {
            Util.messagePlayer(player, ChatColor.RED,
                               "\"" + name + "\"" + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
            return;
        }

        if (isPlayerBusy(player))
            return;

        // These are disabled.
        if (type.equals(DoorType.FLAG) || type.equals(DoorType.ELEVATOR)) // DISABLED ELEVATORS
            return;

        ToolUser tu = type == DoorType.DOOR ? new DoorCreator(plugin, player, name) :
            type == DoorType.DRAWBRIDGE ? new DrawbridgeCreator(plugin, player, name) :
            type == DoorType.PORTCULLIS ? new PortcullisCreator(plugin, player, name) :
            type == DoorType.ELEVATOR ? new ElevatorCreator(plugin, player, name) :
            type == DoorType.FLAG ? new FlagCreator(plugin, player, name) :
            type == DoorType.SLIDINGDOOR ? new SlidingDoorCreator(plugin, player, name) : null;

        startTimerForAbortable(tu, 120 * 20);
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
        startTimerForAbortable((new WaitForSetBlocksToMove(plugin, player, doorUID)),
                               plugin.getConfigLoader().commandWaiterTimeout() * 20);
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
        startTimerForAbortable((new WaitForSetTime(plugin, player, doorUID)),
                               plugin.getConfigLoader().commandWaiterTimeout() * 20);
    }

    public void startAddOwner(Player player, long doorUID)
    {
        replaceWaitForCommand(player);
        startTimerForAbortable((new WaitForAddOwner(plugin, player, doorUID)),
                               plugin.getConfigLoader().commandWaiterTimeout() * 20);
    }

    public void startRemoveOwner(Player player, long doorUID)
    {
        replaceWaitForCommand(player);
        startTimerForAbortable((new WaitForRemoveOwner(plugin, player, doorUID)),
                               plugin.getConfigLoader().commandWaiterTimeout() * 20);
    }

    private boolean isPlayerBusy(Player player)
    {
        boolean isBusy = (plugin.getToolUser(player) != null || plugin.getCommandWaiter(player) != null);
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
        startTimerForAbortable(new PowerBlockRelocator(plugin, player, doorUID),
                               plugin.getConfigLoader().commandWaiterTimeout() * 20);
    }

    public void killAllBigDoorsEntities()
    {
        for (World world : Bukkit.getWorlds())
            for (Entity entity : world.getEntities())
                if (entity.getCustomName() != null && entity.getCustomName().equals("BigDoorsEntity"))
                    entity.remove();
    }

    private static final String upgradeWarning = "\n"
        + "===================================================================================\n"
        + "===================================== WARNING =====================================\n"
        + "===================================================================================\n"
        + "||  Are you absolutely sure you want to upgrade the database to v2 of BigDoors?  ||\n"
        + "||  You will NOT BE ABLE to use this database on the current version anymore!    ||\n"
        + "||  If you are aware of the consequences, execute the following command:         ||\n"
        + "||  \"bigdoors upgradedatabaseforv2 confirm\"                                      ||\n"
        + "===================================================================================\n"
        + "===================================== WARNING =====================================\n"
        + "===================================================================================";

    private void prepareDatabaseForV2(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            ((Player) sender).sendMessage(ChatColor.RED + "Players cannot execute this command!");
            return;
        }
        if (sender instanceof Entity || sender instanceof BlockCommandSender)
            return;

        if (args.length == 2 && args[1].equals("confirm"))
            plugin.getCommander().prepareDatabaseForV2();
        else
            plugin.getMyLogger().logMessageToConsoleOnly(upgradeWarning);
    }

    // Handle commands.
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player player = sender instanceof Player ? (Player) sender : null;

        if (cmd.getName().equalsIgnoreCase("killbigdoorsentities"))
        {
            killAllBigDoorsEntities();
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "All entities have been removed!");
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("bigdoors"))
        {
            String firstCommand = args.length == 0 ? "" : args[0].toLowerCase();

            switch (firstCommand)
            {
            case "upgradedatabaseforv2":
                if (player != null) // Only the server may use this command.
                    break;
                prepareDatabaseForV2(sender, cmd, label, args);
                break;
            case "version":
                if (player != null && !player.hasPermission("bigdoors.admin.version"))
                    break;
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "This server uses version "
                    + plugin.getDescription().getVersion() + " of this plugin!");
                break;

            case "menu":
                if (player != null)
                    plugin.addGUIUser(new GUI(plugin, player));
                break;

            case "reload":
            case "restart":
                if (player != null && !player.hasPermission("bigdoors.admin.restart"))
                    break;
                plugin.restart();
                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN,
                                                    "BigDoors has been restarted!");
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
                if (player == null || !player.hasPermission("bigdoors.user.addowner")) // TODO: Surely, the server
                                                                                       // should be able to add any user
                                                                                       // to any door?
                    break;

                if (args.length > 1)
                {
                    WaitForCommand cw = plugin.getCommandWaiter(player);
                    if (cw != null)
                        return cw.executeCommand(args);

                    Door door = plugin.getCommander().getDoor(args[1], player);
                    if (door == null)
                    {
                        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[1] + "\" "
                            + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
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
                            plugin.getMyLogger()
                                .returnToSender(sender, Level.INFO, ChatColor.RED,
                                                plugin.getMessages().getString("COMMAND.AddOwner.Success"));
                            return true;
                        }
                        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                            plugin.getMessages().getString("COMMAND.AddOwner.Fail"));
                        return false;
                    }
                    plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                        plugin.getMessages().getString("GENERAL.PlayerNotFound")
                                                            + ": \"" + args[2] + "\"");
                    return true;
                }
                // TODO: Print help menu!
                Util.broadcastMessage("FAIL");
                return true;

            case "removeowner":
                if (player == null || !player.hasPermission("bigdoors.user.removeowner"))
                    break;

                if (args.length == 2)
                {
                    WaitForCommand cw = plugin.getCommandWaiter(player);
                    if (cw != null)
                        return cw.executeCommand(args);
                }
                else if (args.length > 2)
                {
                    // If the player is currently in a commandWaiter, just abort that and use the
                    // direct one instead.
                    WaitForCommand cw = plugin.getCommandWaiter(player);
                    if (cw != null && cw.getCommand().equals("removeowner"))
                        cw.abortSilently();

                    Door door = plugin.getCommander().getDoor(args[1], player);
                    if (door == null)
                    {
                        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[1] + "\" "
                            + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
                        return true;
                    }

                    UUID playerUUID = Util.playerUUIDFromString(args[2]);

                    if (playerUUID != null)
                    {
                        if (plugin.getCommander().removeOwner(door, playerUUID, player))
                        {
                            plugin.getMyLogger()
                                .returnToSender(sender, Level.INFO, ChatColor.RED,
                                                plugin.getMessages().getString("COMMAND.RemoveOwner.Success"));
                            return true;
                        }
                        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                            plugin.getMessages().getString("COMMAND.RemoveOwner.Fail"));
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
            doorDebug(player, args);
            return true;
        }

        // /setautoclosetime <doorName> <time>
        if (cmd.getName().equalsIgnoreCase("setautoclosetime"))
        {
            // If there is only 1 argument, assume that it is a player using the
            // commandWaiter system
            // And therefore the Door is already defined somewhere else (e.g. selected from
            // the GUI).
            if (args.length == 1 && player != null)
            {
                WaitForCommand cw = plugin.getCommandWaiter(player);
                if (cw != null)
                    return cw.executeCommand(args);
            }
            else if (args.length == 2)
            {
                // If the player is currently in a commandWaiter, just abort that and use the
                // direct one instead.
                WaitForCommand cw = plugin.getCommandWaiter(player);
                if (cw != null && cw.getCommand().equals("setautoclosetime"))
                    cw.abortSilently();

                Door door = plugin.getCommander().getDoor(args[0], player);
                if (door == null)
                    return false;

                if (!plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.CHANGETIMER))
                    return true;

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
            // If there is only 1 argument, assume that it is a player using the
            // commandWaiter system
            // And therefore the Door is already defined somewhere else (e.g. selected from
            // the GUI).
            if (args.length == 1 && player != null)
            {
                WaitForCommand cw = plugin.getCommandWaiter(player);
                if (cw != null)
                    return cw.executeCommand(args);
            }
            else if (args.length == 2)
            {
                // If the player is currently in a commandWaiter, just abort that and use the
                // direct one instead.
                WaitForCommand cw = plugin.getCommandWaiter(player);
                if (cw != null && cw.getCommand().equals("setblockstomove"))
                    cw.abortSilently();

                Door door = plugin.getCommander().getDoor(args[0], player);
                if (door == null)
                    return false;

                if (!plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(),
                                                                  DoorAttribute.BLOCKSTOMOVE))
                    return true;

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

            if (!plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(),
                                                              DoorAttribute.DIRECTION_ROTATE))
                return true;

            RotateDirection openDir = null;
            if (door.getType() == DoorType.SLIDINGDOOR)
            {
                if (args[1].equalsIgnoreCase("NORTH"))
                    openDir = RotateDirection.NORTH;
                else if (args[1].equalsIgnoreCase("EAST"))
                    openDir = RotateDirection.NORTH;
                else if (args[1].equalsIgnoreCase("SOUTH"))
                    openDir = RotateDirection.NORTH;
                else if (args[1].equalsIgnoreCase("WEST"))
                    openDir = RotateDirection.NORTH;
            }
            else if (door.getType() == DoorType.ELEVATOR)
            {
                if (args[1].equalsIgnoreCase("UP"))
                    openDir = RotateDirection.UP;
                else if (args[1].equalsIgnoreCase("DOWN"))
                    openDir = RotateDirection.DOWN;
            }
            else
            {
                if (args[1].equalsIgnoreCase("CLOCK") || args[1].equalsIgnoreCase("CLOCKWISE"))
                    openDir = RotateDirection.CLOCKWISE;
                else if (args[1].equalsIgnoreCase("COUNTER") || args[1].equalsIgnoreCase("COUNTERCLOCKWISE"))
                    openDir = RotateDirection.COUNTERCLOCKWISE;
            }

            if (openDir == null)
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
            door.getPowerBlockLoc().getBlock().setType(plugin.getConfigLoader().getPowerBlockTypes().iterator().next());
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("recalculatepowerblocks"))
        {
            plugin.getCommander().recalculatePowerBlockHashes();
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN,
                                                "power block hashes have been regenerated!");
            return true;
        }

        // /pausedoors
        if (cmd.getName().equalsIgnoreCase("pausedoors"))
        {
            plugin.getCommander().togglePaused();
            return true;
        }

        // /pausedoors
        if (cmd.getName().equalsIgnoreCase("shadowtoggledoor"))
        {
            if (args.length != 1)
                return false;

            Door door = plugin.getCommander().getDoor(args[0], player);
            DoorOpenResult result = plugin.getDoorOpener(door.getType()).shadowToggle(door);
            String resultMsg = result.equals(DoorOpenResult.SUCCESS) ?
                "Successfully shadowtoggled door " + door.getDoorUID() :
                plugin.getMessages().getString(DoorOpenResult.getMessage(result));
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.YELLOW, resultMsg);
            return true;
        }

        // /opendoor <doorName 1> [doorName 2] ... [doorName x] [time]
        if (cmd.getName().equalsIgnoreCase("opendoor") || cmd.getName().equalsIgnoreCase("closedoor") ||
            cmd.getName().equalsIgnoreCase("toggledoor"))
        {
            boolean instantly = false;
            // type0 = opendoor, type1 = closedoor, type2 = toggledoor
            int type = cmd.getName().equalsIgnoreCase("opendoor") ? 0 :
                cmd.getName().equalsIgnoreCase("closedoor") ? 1 : 2;

            if (args.length >= 1)
            {
                String lastStr = args[args.length - 1];
                // Last argument sets speed if it's a double.
                double time = Util.longFromString(lastStr, -1L) == -1L ? Util.doubleFromString(lastStr, 0.0D) : 0.0D;
                int endIDX = args.length;
                // If the time variable was specified, decrement endIDX by 1, as the last
                // argument is not a door!
                if (time != 0.0D)
                {
                    --endIDX;
                    if (time < 0)
                        instantly = true;
                }

                // Go over all arguments until the last one (or the one before that).
                for (int index = 0; index < endIDX; ++index)
                {
                    Door door = plugin.getCommander().getDoor(args[index], player);

                    // If the door is null, let the player know that the selected door is invalid.
                    if (door == null)
                    {
                        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[index]
                            + "\" " + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
                        // If player is null, the console or a command block is being used.
                        // If that's the case, kindly remind them (in the console) that they should be
                        // using DoorUIDs.
                        if (player == null)
                            plugin.getMyLogger()
                                .returnToSender(sender, Level.INFO, ChatColor.RED,
                                                "Don't forget that you should use the DoorUID in the console/command blocks! DoorName won't work here!");
                    }
                    else
                    {
                        if (type == 2 || door.getOpenDir().equals(RotateDirection.NONE))
                            openDoorCommand(sender, door, time, instantly);
                        else if (type == 1)
                        {
                            if (door.isOpen())
                                openDoorCommand(sender, door, time, instantly);
                            else
                                plugin.getMyLogger()
                                    .returnToSender(sender, Level.INFO, ChatColor.RED,
                                                    plugin.getMessages().getString("GENERAL.Door") + " \"" + args[index]
                                                        + "\" "
                                                        + plugin.getMessages().getString("GENERAL.DoorAlreadyClosed"));
                        }
                        else if (type == 0)
                            if (!door.isOpen())
                                openDoorCommand(sender, door, time, instantly);
                            else
                                plugin.getMyLogger()
                                    .returnToSender(sender, Level.INFO, ChatColor.RED,
                                                    plugin.getMessages().getString("GENERAL.Door") + " \"" + args[index]
                                                        + "\" "
                                                        + plugin.getMessages().getString("GENERAL.DoorAlreadyOpen"));
                    }
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
                startTimerForAbortable(new PowerBlockInspector(plugin, player, -1),
                                       plugin.getConfigLoader().commandWaiterTimeout() * 20);
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
                if (plugin.getCommander().hasPermissionForAction(player, doorUID, DoorAttribute.RELOCATEPOWERBLOCK))
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
                    plugin.getMyLogger().returnToSender(player, Level.INFO, ChatColor.RED,
                                                        plugin.getMessages().getString("CREATOR.GENERAL.Cancelled"));
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

    public void delDoor(Player player, String doorName)
    {
        long doorUID = 0;
        try
        {
            doorUID = Long.parseLong(doorName);
        }
        catch (NumberFormatException e)
        {
        }

        long doorCount = countDoors(player, doorName);
        if (doorCount == 0)
        {
            Util.messagePlayer(player, ChatColor.RED, "No door found by that name!");
            return;
        }
        else if (doorCount == 1)
        {
            doorUID = plugin.getCommander().getDoor(doorName, player).getDoorUID();
        }
        else
        {
            Util.messagePlayer(player, ChatColor.RED,
                               "More than one door found with that name! Please use their ID instead:");
            listDoors(player, doorName);
            return;
        }

        if (plugin.getCommander().removeDoor(player, doorUID))
            Util.messagePlayer(player, ChatColor.GREEN, "Door deleted!");
    }

    // Used for various debugging purposes (you don't say!).
    public void doorDebug(Player player, String[] args)
    {
//        UUID pim = UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935");
//        int maxSize = plugin.getVaultManager().getMaxDoorSizeForPlayer(Bukkit.getWorld("world"), pim);
//        System.out.println("Max door size for Pim is: " + maxSize);
//
//        if (args.length > 0)
//        {
//            String node = args[0];
//            int highest = plugin.getVaultManager().getHighestPermissionSuffix(Bukkit.getWorld("world"), Bukkit.getOfflinePlayer(pim), node);
//            System.out.println("Highest value of \"" + node + "\" for Pim is: " + highest);
//        }
//
//        if (player != null)
//        {
//            maxSize = plugin.getVaultManager().getMaxDoorSizeForPlayer(player.getWorld(), pim);
//            System.out.println("ONLINE: Max door size for Pim is: " + maxSize);
//
//            if (args.length > 0)
//            {
//                String node = args[0];
//                int highest = plugin.getVaultManager().getHighestPermissionSuffix(player.getWorld(), player, node);
//                System.out.println("Highest value of \"" + node + "\" for Pim is: " + highest);
//            }
//        }

//        plugin.getUpdateManager().checkForUpdates();
//
//        new BukkitRunnable()
//        {
//            @Override
//            public void run()
//            {
//                player.sendMessage(plugin.getLoginMessage());
//            }
//        }.runTaskLater(plugin, 20L);

//        Location loc = new Location(player.getWorld(), 128, 75, 140);
//        long toSecond = 1000000000L;
//        long secondstToRun = 10L;
//        new BukkitRunnable()
//        {
//            long startTime = System.nanoTime();
//            long count = 0;
//            long toggles = 0;
//            long seconds = 0;
//            long lastDebugSecond = 0;
//
//            @Override
//            public void run()
//            {
//                long currentTime = System.nanoTime();
//                long delta = currentTime - startTime;
//                seconds = delta / toSecond;
//
//                if (seconds >= lastDebugSecond)
//                {
//                    String message = String.format("Activated the powerblock %d times in %d ns (%d s). This resulted in %d toggles.",
//                                                   count, delta, seconds, toggles);
//                    System.out.println(message);
//                    if (seconds == secondstToRun)
//                        Bukkit.broadcastMessage(message);
//                    ++lastDebugSecond;
//                }
//
//                if (seconds >= 10)
//                    cancel();
//                else
//                {
//                    for (int i = 0; i < 100; ++i)
//                    {
//                        if (plugin.getRedstoneHandler().checkDoor(loc))
//                            ++toggles;
//                        ++count;
//                    }
//                }
//            }
//        }.runTaskTimer(plugin, 0, 1);
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
        String help = player == null ? "\n" : "";
        String commandPrefix = player == null ? "" : "/";
        help += ChatColor.GREEN + "====[ BigDoors Help ]====\n";
        help += helpFormat(commandPrefix + "BigDoors menu", "Opens BigDoors' GUI.");
        help += helpFormat(commandPrefix + "BigDoors version", "Get the version of this plugin.");
        help += helpFormat(commandPrefix + "BigDoors removeowner <door> <player>", "Add another owner for a door.");
        help += helpFormat(commandPrefix + "BigDoors addowner <door> <player> [permission]",
                           "Add another owner for a door.");
        if (player == null || player.hasPermission("bigdoors.admin.restart"))
            help += helpFormat("BigDoors restart", "Restart the plugin. Reinitializes almost everything.");
        if (player == null || player.hasPermission("bigdoors.admin.stopdoors"))
            help += helpFormat("BigDoors stop", "Forces all doors to finish instantly.");
        if (player == null || player.hasPermission("bigdoors.admin.pausedoors"))
            help += helpFormat("BigDoors pause", "Pauses all door movement until the command is run again.");
        if (player == null)
            help += helpFormat("BigDoors upgradedatabaseforv2", "Prepares the database for v2 of BigDoors.");

        // Remove color codes for console.
        if (player == null)
            help = ChatColor.stripColor(help);
        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, help);
    }
}
