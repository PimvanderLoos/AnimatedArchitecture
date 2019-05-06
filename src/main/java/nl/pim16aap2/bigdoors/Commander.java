package nl.pim16aap2.bigdoors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.toolusers.DoorCreator;
import nl.pim16aap2.bigdoors.toolusers.DrawbridgeCreator;
import nl.pim16aap2.bigdoors.toolusers.ElevatorCreator;
import nl.pim16aap2.bigdoors.toolusers.FlagCreator;
import nl.pim16aap2.bigdoors.toolusers.PortcullisCreator;
import nl.pim16aap2.bigdoors.toolusers.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.toolusers.SlidingDoorCreator;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.util.Abortable;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorDirection;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.XMaterial;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForAddOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForRemoveOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetBlocksToMove;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetTime;

public class Commander
{
    private final BigDoors plugin;
    private HashSet<Long> busyDoors;
    private HashMap<UUID, String> players;
    private boolean goOn   = true;
    private boolean paused = false;
    private SQLiteJDBCDriverConnection db;
    private Messages messages;

    public Commander(BigDoors plugin, SQLiteJDBCDriverConnection db)
    {
        this.plugin = plugin;
        this.db     = db;
        busyDoors   = new HashSet<>();
        messages    = plugin.getMessages();
        players     = new HashMap<>();
    }

    // Check if a door is busy
    public boolean isDoorBusy(long doorUID)
    {
        return busyDoors.contains(doorUID);
    }

    public void emptyBusyDoors()
    {
        busyDoors.clear();
    }

    // Change the busy-status of a door.
    public void setDoorBusy(long doorUID)
    {
        busyDoors.add(doorUID);
    }

    // Set the availability of the door.
    public void setDoorAvailable(long doorUID)
    {
        busyDoors.remove(doorUID);
    }

    public void setDoorOpenTime(long doorUID, int autoClose)
    {
        updateDoorAutoClose(doorUID, autoClose);
    }

    private boolean isPlayerBusy(Player player)
    {
        boolean isBusy = (plugin.getToolUser(player) != null || plugin.isCommandWaiter(player) != null);
        if (isBusy)
            Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.IsBusy"));
        return isBusy;
    }

    public void stopDoors()
    {
        setCanGo(false);
        emptyBusyDoors();
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                setCanGo(true);
            }
        }.runTaskLater(plugin, 5L);
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

        if (name != null && !Util.isValidDoorName(name))
        {
            Util.messagePlayer(player, ChatColor.RED,
                               "\"" + name + "\"" + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
            return;
        }

        if (isPlayerBusy(player))
            return;

        // These are disabled.
        if (type == DoorType.FLAG)
            return;

        ToolUser tu = type == DoorType.DOOR ? new DoorCreator(plugin, player, name) :
                           type == DoorType.DRAWBRIDGE ? new DrawbridgeCreator(plugin, player, name) :
                           type == DoorType.PORTCULLIS ? new PortcullisCreator(plugin, player, name) :
                           type == DoorType.ELEVATOR ? new ElevatorCreator(plugin, player, name) :
                           type == DoorType.FLAG ? new FlagCreator(plugin, player, name) :
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

//    public void listDoorInfo(Player player, Door door)
//    {
//
//    }

    public void startPowerBlockRelocator(Player player, Door door)
    {
        startTimerForAbortable(new PowerBlockRelocator(plugin, player, door.getDoorUID()), 20 * 20);
    }

    public void startTimerSetter(Player player, Door door)
    {
        plugin.addCommandWaiter(new WaitForSetTime(plugin, (SubCommandSetAutoCloseTime) plugin.getCommand("BigDoors", "setautoclosetime"), player, door));
    }

    public void startBlocksToMoveSetter(Player player, Door door)
    {
        plugin.addCommandWaiter(new WaitForSetBlocksToMove(plugin, (SubCommandSetBlocksToMove) plugin.getCommand("BigDoors", "setblockstomove"), player, door));
    }

    public void startAddOwner(Player player, Door door)
    {
        plugin.addCommandWaiter(new WaitForAddOwner(plugin, (SubCommandAddOwner) plugin.getCommand("BigDoors", "addowner"), player, door));
    }

    public void startRemoveOwner(Player player, Door door)
    {
        plugin.addCommandWaiter(new WaitForRemoveOwner(plugin, (SubCommandRemoveOwner) plugin.getCommand("BigDoors", "removeowner"), player, door));
    }

    public void setDoorBlocksToMove(long doorUID, int autoClose)
    {
        plugin.getCommander().updateDoorBlocksToMove(doorUID, autoClose);
    }

    // Check if the doors are paused.
    public boolean isPaused()
    {
        return paused;
    }

    // Toggle the paused status of all doors.
    public void togglePaused()
    {
        paused = !paused;
    }

    // Check if the doors can go. This differs from begin paused in that it will finish up
    // all currently moving doors.
    public boolean canGo()
    {
        return goOn;
    }

    // Change the canGo status of all doors.
    public void setCanGo(boolean bool)
    {
        goOn = bool;
    }

    // Print an ArrayList of doors to a player.
    public void printDoors(Player player, ArrayList<Door> doors)
    {
        for (Door door : doors)
            Util.messagePlayer(player, door.getDoorUID() + ": " + door.getName().toString());
    }

    public Door getDoor(long doorUID)
    {
        return db.getDoor(null, doorUID);
    }

    // Get the door from the string. Can be use with a doorUID or a doorName.
    public Door getDoor(String doorStr, @Nullable Player player)
    {
        // First try converting the doorStr to a doorUID.
        try
        {
            long doorUID = Long.parseLong(doorStr);
            return db.getDoor(player == null ? null : player.getUniqueId() , doorUID);
        }
        // If it can't convert to a long, get all doors from the player with the provided name.
        // If there is more than one, tell the player that they are going to have to make a choice.
        catch (NumberFormatException e)
        {
            if (player == null)
                return null;
            ArrayList<Door> doors = new ArrayList<>();
            doors = db.getDoors(player.getUniqueId().toString(), doorStr);
            if (doors.size() == 1)
                return doors.get(0);

            if (doors.size() == 0)
                Util.messagePlayer(player, messages.getString("GENERAL.NoDoorsFound"));
            else
                Util.messagePlayer(player, messages.getString("GENERAL.MoreThan1DoorFound"));
            printDoors(player, doors);
            return null;
        }
    }

    public void addDoor(Door newDoor)
    {
        db.insert(newDoor);
    }

    public void addDoor(Door newDoor, Player player, int permission)
    {
        if (newDoor.getPlayerUUID() != player.getUniqueId())
            newDoor.setPlayerUUID(player.getUniqueId());
        if (newDoor.getPermission() != permission)
            newDoor.setPermission(permission);
        db.insert(newDoor);
    }

    // Add a door to the db of doors.
    public void addDoor(Door newDoor, Player player)
    {
        addDoor(newDoor, player, 0);
    }

    public void removeDoor(long doorUID)
    {
        db.removeDoor(doorUID);
    }

    public void removeDoor(String playerUUID, String doorName)
    {
        db.removeDoor(playerUUID, doorName);
    }

    // Returns the number of doors owner by a player and with a specific name, if provided (can be null).
    public int countDoors(String playerUUID, @Nullable String doorName)
    {
        return db.countDoors(playerUUID, doorName);
    }

    public int countDoors(String doorName)
    {
        // TODO: This is dumb.
        return getDoors(doorName).size();
    }

    // Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
    public ArrayList<Door> getDoors(String playerUUID, @Nullable String name)
    {
        return db.getDoors(playerUUID, name);
    }

    // Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null),
    // and where the player has a higher permission node (lower number) than specified.
    public ArrayList<Door> getDoors(String playerUUID, @Nullable String name, int maxPermission)
    {
        return db.getDoors(playerUUID, name, maxPermission);
    }

    // Returns an ArrayList of doors with a specific name.
    public ArrayList<Door> getDoors(String name)
    {
        return db.getDoors(name);
    }

    // Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
    public ArrayList<Door> getDoorsInRange(String playerUUID, @Nullable String name, int start, int end)
    {
        return db.getDoors(playerUUID, name, start, end, Integer.MAX_VALUE);
    }

    public void fillDoor(Door door)
    {
        for (int i = door.getMinimum().getBlockX(); i <= door.getMaximum().getBlockX(); ++i)
            for (int j = door.getMinimum().getBlockY(); j <= door.getMaximum().getBlockY(); ++j)
                for (int k = door.getMinimum().getBlockZ(); k <= door.getMaximum().getBlockZ(); ++k)
                    door.getWorld().getBlockAt(i, j, k).setType(XMaterial.STONE.parseMaterial());
    }

    public UUID playerUUIDFromName(String playerName)
    {
        UUID uuid = players.entrySet().stream()
            .filter(e -> e.getValue().equals(playerName))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
        if (uuid != null)
            return uuid;

        uuid = db.getUUIDFromName(playerName);
        if (uuid != null)
            return uuid;

        uuid = Util.playerUUIDFromString(playerName);
        if (uuid != null)
            updatePlayer(uuid, playerName);
        return uuid;
    }

    public String playerNameFromUUID(UUID playerUUID)
    {
        // Try from HashSet first; it's the fastest.
        if (players.containsKey(playerUUID))
            return players.get(playerUUID);
        // Then try to get it from an online player.
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null)
            return player.getName();
        // First try to get the player name from the database.
        String name = db.getPlayerName(playerUUID);
        if (name != null)
            return name;
        // As a last resort, try to get the name from an offline player. This is slow af, so last resort.
        name = Util.nameFromUUID(playerUUID);
        // Then place the UUID/String combo in the db. Need moar data!
        updatePlayer(playerUUID, name);
        return name;
    }

    public void updatePlayer(UUID uuid, String playerName)
    {
        db.updatePlayerName(uuid, playerName);
        players.put(uuid, playerName);
    }

    public void updatePlayer(Player player)
    {
        updatePlayer(player.getUniqueId(), player.getName());
    }

    public void removePlayer(Player player)
    {
        players.remove(player.getUniqueId());
    }

    // Get a door with a specific doorUID.
    public Door getDoor(@Nullable UUID playerUUID, long doorUID)
    {
        return db.getDoor(playerUUID, doorUID);
    }

//    // Get a door with a specific doorUID.
//    @Deprecated
//    public Door getDoor2(@Nullable UUID playerUUID, long doorUID)
//    {
//        return db.getDoor2(playerUUID, doorUID);
//    }

    public boolean hasPermissionForActionPrintMessage(Player player, long doorUID, DoorAttribute atr)
    {
        return hasPermissionForActionPrintMessage(player.getUniqueId(), doorUID, atr);
    }

    public boolean hasPermissionForActionPrintMessage(UUID playerUUID, long doorUID, DoorAttribute atr)
    {
        boolean hasPermission = hasPermissionForAction(playerUUID, doorUID, atr);
        if (!hasPermission)
            Util.messagePlayer(Bukkit.getPlayer(playerUUID), plugin.getMessages().getString("GENERAL.NoPermissionForAction"));
        return hasPermission;
    }

    public boolean hasPermissionForAction(Player player, long doorUID, DoorAttribute atr)
    {
        return hasPermissionForAction(player.getUniqueId(), doorUID, atr);
    }

    public boolean hasPermissionForAction(UUID playerUUID, long doorUID, DoorAttribute atr)
    {
        int playerPermission = getPermission(playerUUID.toString(), doorUID);
        boolean hasPermission = playerPermission >= 0 && playerPermission <= DoorAttribute.getPermissionLevel(atr);
        return hasPermission;
    }

    // Get the permission of a player on a door.
    public int getPermission(String playerUUID, long doorUID)
    {
        return db.getPermission(playerUUID, doorUID);
    }

    // Update the coordinates of a given door.
    public void updateDoorCoords(long doorUID, boolean isOpen, int blockXMin, int blockYMin,
                                 int blockZMin, int blockXMax, int blockYMax, int blockZMax)
    {
        db.updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax,
                            blockYMax, blockZMax, null);
    }

    // Update the coordinates of a given door.
    public void updateDoorCoords(long doorUID, boolean isOpen, int blockXMin, int blockYMin,
                                 int blockZMin, int blockXMax, int blockYMax, int blockZMax,
                                 DoorDirection newEngSide)
    {
        db.updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax, blockYMax,
                            blockZMax, newEngSide);
    }

    public void addOwner(Door door, UUID playerUUID)
    {
        addOwner(door, playerUUID, 1);
    }

    public boolean addOwner(Door door, UUID playerUUID, int permission)
    {
        if (permission < 1 || permission > 2 || door.getPermission() != 0 || door.getPlayerUUID().equals(playerUUID))
            return false;

        db.addOwner(door.getDoorUID(), playerUUID, permission);
        return true;
    }

    public boolean removeOwner(Door door, UUID playerUUID)
    {
        return removeOwner(door.getDoorUID(), playerUUID);
    }

    public boolean removeOwner(long doorUID, UUID playerUUID)
    {
        if (db.getPermission(playerUUID.toString(), doorUID) == 0)
            return false;
        return db.removeOwner(doorUID, playerUUID);
    }

    public ArrayList<DoorOwner> getDoorOwners(long doorUID, @Nullable UUID playerUUID)
    {
        return db.getOwnersOfDoor(doorUID, playerUUID);
    }

    public void updateDoorOpenDirection(long doorUID, RotateDirection openDir)
    {
        db.updateDoorOpenDirection(doorUID, openDir == null ? RotateDirection.NONE : openDir);
    }

    public void updateDoorAutoClose(long doorUID, int autoClose)
    {
        db.updateDoorAutoClose(doorUID, autoClose);
    }

    public void updateDoorBlocksToMove(long doorID, int blocksToMove)
    {
        db.updateDoorBlocksToMove(doorID, blocksToMove);
    }

    // Change the "locked" status of a door.
    public void setLock(long doorUID, boolean newLockStatus)
    {
        db.setLock(doorUID, newLockStatus);
    }

    // Get a door from the x,y,z coordinates of its power block.
    public Door doorFromPowerBlockLoc(Location loc)
    {
        long chunkHash = Util.chunkHashFromLocation(loc);
        HashMap<Long, Long> powerBlockData = plugin.getPBCache().get(chunkHash);
        if (powerBlockData == null)
        {
            powerBlockData = db.getPowerBlockData(chunkHash);
            plugin.getPBCache().put(chunkHash, powerBlockData);
        }

        Long doorUID = powerBlockData.get(Util.locationHash(loc));
        return doorUID == null ? null : db.getDoor(null, doorUID);
    }

    // Change the location of a powerblock.
    public void updatePowerBlockLoc(long doorUID, Location loc)
    {
        plugin.getPBCache().invalidate(db.getDoor(null, doorUID).getPowerBlockChunkHash());
        db.updateDoorPowerBlockLoc(doorUID, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getUID());
        plugin.getPBCache().invalidate(Util.chunkHashFromLocation(loc));
    }

    public boolean isPowerBlockLocationValid(Location loc)
    {
        return db.isPowerBlockLocationEmpty(loc);
    }
}
