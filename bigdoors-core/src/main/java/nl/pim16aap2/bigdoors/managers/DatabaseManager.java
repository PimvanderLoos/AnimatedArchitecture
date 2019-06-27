package nl.pim16aap2.bigdoors.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.Abortable;
import nl.pim16aap2.bigdoors.spigotutil.DoorAttribute;
import nl.pim16aap2.bigdoors.spigotutil.DoorOwner;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.toolusers.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.TimedMapCache;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForAddOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForRemoveOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetBlocksToMove;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetTime;

public class DatabaseManager extends Restartable
{
    private HashSet<Long> busyDoors;
    // Players map stores players for faster UUID / Name matching.
    private TimedMapCache<UUID, String> players;
    private boolean goOn = true;
    private boolean paused = false;
    private SQLiteJDBCDriverConnection db;
    private Messages messages;
    private final BigDoors plugin;

    public DatabaseManager(final BigDoors plugin, SQLiteJDBCDriverConnection db)
    {
        super(plugin);
        this.db = db;
        this.plugin = plugin;
        busyDoors = new HashSet<>();
        messages = plugin.getMessages();
        players = new TimedMapCache<>(plugin, HashMap::new, 1400);
    }

    @Override
    public void restart()
    {
        busyDoors.clear();
        messages = plugin.getMessages();
    }

    public boolean isDoorBusy(long doorUID)
    {
        return busyDoors.contains(doorUID);
    }

    public void emptyBusyDoors()
    {
        busyDoors.clear();
    }

    public void setDoorBusy(long doorUID)
    {
        busyDoors.add(doorUID);
    }

    public void setDoorAvailable(long doorUID)
    {
        busyDoors.remove(doorUID);
    }

    public void setDoorOpenTime(long doorUID, int autoClose)
    {
        updateDoorAutoClose(doorUID, autoClose);
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

    public UUID getPlayerUUIDFromString(String playerStr)
    {
        UUID playerUUID = SpigotUtil.playerUUIDFromString(playerStr);
        if (playerUUID == null)
            playerUUID = db.getPlayerUUID(playerStr);
        return playerUUID;
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

    public void startPowerBlockRelocator(Player player, DoorBase door)
    {
        startTimerForAbortable(new PowerBlockRelocator(plugin, player, door.getDoorUID()), 20 * 20);
    }

    public void startTimerSetter(Player player, DoorBase door)
    {
        startTimerForAbortable(new WaitForSetTime(plugin, (SubCommandSetAutoCloseTime) plugin.getCommand(CommandData.SETAUTOCLOSETIME), player, door), 20 * 20);
    }

    public void startBlocksToMoveSetter(Player player, DoorBase door)
    {
        startTimerForAbortable(new WaitForSetBlocksToMove(plugin, (SubCommandSetBlocksToMove) plugin.getCommand(CommandData.SETBLOCKSTOMOVE), player, door), 20 * 20);
    }

    public void startAddOwner(Player player, DoorBase door)
    {
        startTimerForAbortable(new WaitForAddOwner(plugin, (SubCommandAddOwner) plugin.getCommand(CommandData.ADDOWNER), player, door), 20 * 20);
    }

    public void startRemoveOwner(Player player, DoorBase door)
    {
        startTimerForAbortable(new WaitForRemoveOwner(plugin, (SubCommandRemoveOwner) plugin.getCommand(CommandData.REMOVEOWNER), player, door), 20 * 20);
    }

    public void setDoorBlocksToMove(long doorUID, int autoClose)
    {
        plugin.getDatabaseManager().updateDoorBlocksToMove(doorUID, autoClose);
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
    public void printDoors(Player player, ArrayList<DoorBase> doors)
    {
        for (DoorBase door : doors)
            SpigotUtil.messagePlayer(player, door.getDoorUID() + ": " + door.getName().toString());
    }

    public DoorBase getDoor(long doorUID)
    {
        return db.getDoor(null, doorUID);
    }

    // Get the door from the string. Can be use with a doorUID or a doorName.
    public DoorBase getDoor(String doorStr, Player player)
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
            ArrayList<DoorBase> doors = new ArrayList<>();
            doors = db.getDoors(player.getUniqueId().toString(), doorStr);
            if (doors.size() == 1)
                return doors.get(0);

            if (doors.size() == 0)
                SpigotUtil.messagePlayer(player, messages.getString("GENERAL.NoDoorsFound"));
            else
                SpigotUtil.messagePlayer(player, messages.getString("GENERAL.MoreThan1DoorFound"));
            printDoors(player, doors);
            return null;
        }
    }

    public void addDoorBase(DoorBase newDoor)
    {
        db.insert(newDoor);
    }

    public void addDoorBase(DoorBase newDoor, Player player, int permission)
    {
        newDoor.setDoorOwner(new DoorOwner(newDoor.getDoorUID(), player.getUniqueId(), player.getName(), permission));
        addDoorBase(newDoor);
    }

    // Add a door to the db of doors.
    public void addDoorBase(DoorBase newDoor, Player player)
    {
        addDoorBase(newDoor, player, 0);
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
    public int countDoors(String playerUUID, String doorName)
    {
        return db.countDoors(playerUUID, doorName);
    }

    public int countDoors(String doorName)
    {
        // TODO: This is dumb.
        return getDoors(doorName).size();
    }

    // Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
    public ArrayList<DoorBase> getDoors(String playerUUID, String name)
    {
        return db.getDoors(playerUUID, name);
    }

    // Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null),
    // and where the player has a higher permission node (lower number) than specified.
    public ArrayList<DoorBase> getDoors(String playerUUID, String name, int maxPermission)
    {
        return db.getDoors(playerUUID, name, maxPermission);
    }

    // Returns an ArrayList of doors with a specific name.
    public ArrayList<DoorBase> getDoors(String name)
    {
        return db.getDoors(name);
    }

    public void fillDoor(DoorBase door)
    {
        for (int i = door.getMinimum().getBlockX(); i <= door.getMaximum().getBlockX(); ++i)
            for (int j = door.getMinimum().getBlockY(); j <= door.getMaximum().getBlockY(); ++j)
                for (int k = door.getMinimum().getBlockZ(); k <= door.getMaximum().getBlockZ(); ++k)
                    door.getWorld().getBlockAt(i, j, k).setType(Material.STONE);
    }

    public UUID playerUUIDFromName(String playerName)
    {
        String uuidStr = players.get(playerName);
        if (uuidStr != null)
            return UUID.fromString(uuidStr);

        UUID uuid = db.getUUIDFromName(playerName);
        if (uuid != null)
            return uuid;

        uuid = SpigotUtil.playerUUIDFromString(playerName);
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
        name = SpigotUtil.nameFromUUID(playerUUID);
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
    public DoorBase getDoor(UUID playerUUID, long doorUID)
    {
        return db.getDoor(playerUUID, doorUID);
    }

//    // Get a door with a specific doorUID.
//    @Deprecated
//    public Door getDoor2(UUID playerUUID, long doorUID)
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
            SpigotUtil.messagePlayer(Bukkit.getPlayer(playerUUID), plugin.getMessages().getString("GENERAL.NoPermissionForAction"));
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
                                 MyBlockFace newEngSide)
    {
        db.updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax, blockYMax,
                            blockZMax, newEngSide);
    }

    public void addOwner(DoorBase door, UUID playerUUID)
    {
        addOwner(door, playerUUID, 1);
    }

    public boolean addOwner(DoorBase door, UUID playerUUID, int permission)
    {
        if (permission < 1 || permission > 2 || door.getPermission() != 0 || door.getPlayerUUID().equals(playerUUID))
            return false;

        db.addOwner(door.getDoorUID(), playerUUID, permission);
        return true;
    }

    public boolean removeOwner(DoorBase door, UUID playerUUID)
    {
        return removeOwner(door.getDoorUID(), playerUUID);
    }

    public boolean removeOwner(long doorUID, UUID playerUUID)
    {
        if (db.getPermission(playerUUID.toString(), doorUID) == 0)
            return false;
        return db.removeOwner(doorUID, playerUUID);
    }

    public ArrayList<DoorOwner> getDoorOwners(long doorUID, UUID playerUUID)
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
    public DoorBase doorFromPowerBlockLoc(Location loc)
    {
        long chunkHash = SpigotUtil.chunkHashFromLocation(loc);
        HashMap<Long, Long> powerBlockData = plugin.getPBCache().get(chunkHash);
        if (powerBlockData == null)
        {
            powerBlockData = db.getPowerBlockData(chunkHash);
            plugin.getPBCache().put(chunkHash, powerBlockData);
        }

        Long doorUID = powerBlockData.get((long) loc.hashCode());
        return doorUID == null ? null : db.getDoor(null, doorUID);
    }

    // Change the location of a powerblock.
    public void updatePowerBlockLoc(long doorUID, Location loc)
    {
        plugin.getPBCache().remove(db.getDoor(null, doorUID).getPowerBlockChunkHash());
        db.updateDoorPowerBlockLoc(doorUID, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getUID());
        plugin.getPBCache().remove(SpigotUtil.chunkHashFromLocation(loc));
    }

    public boolean isPowerBlockLocationValid(Location loc)
    {
        return db.isPowerBlockLocationEmpty(loc);
    }
}
