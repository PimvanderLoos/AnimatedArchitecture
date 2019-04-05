package nl.pim16aap2.bigDoors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.util.DoorAttribute;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOwner;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

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
            ArrayList<Door> doors = new ArrayList<Door>();
            doors = db.getDoors(player == null ? null : player.getUniqueId().toString(), doorStr);
            if (doors.size() == 1)
                return doors.get(0);
            else
            {
                if (doors.size() == 0)
                    Util.messagePlayer(player, messages.getString("GENERAL.NoDoorsFound"));
                else
                    Util.messagePlayer(player, messages.getString("GENERAL.MoreThan1DoorFound"));
                printDoors(player, doors);
                return null;
            }
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
    public long countDoors(String playerUUID, @Nullable String doorName)
    {
        return db.countDoors(playerUUID, doorName);
    }

    // Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
    public ArrayList<Door> getDoors(String playerUUID, @Nullable String name)
    {
        return playerUUID == null ? getDoors(name) : db.getDoors(playerUUID, name);
    }

    // Returns an ArrayList of doors with a specific name.
    private ArrayList<Door> getDoors(String name)
    {
        return db.getDoors(name);
    }

    // Returns an ArrayList of doors owner by a player and with a specific name, if provided (can be null).
    public ArrayList<Door> getDoorsInRange(String playerUUID, @Nullable String name, int start, int end)
    {
        return db.getDoors(playerUUID, name, start, end);
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

    public boolean hasPermissionForAction(Player player, long doorUID, DoorAttribute atr)
    {
        int playerPermission = getPermission(player.getUniqueId().toString(), doorUID);
        boolean hasPermission = playerPermission >= 0 && playerPermission <= DoorAttribute.getPermissionLevel(atr);
        if (!hasPermission)
            Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.NoPermissionForAction"));
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

    public void addOwner(UUID playerUUID, Door door)
    {
        addOwner(playerUUID, door, 1);
    }

    public boolean addOwner(UUID playerUUID, Door door, int permission)
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
