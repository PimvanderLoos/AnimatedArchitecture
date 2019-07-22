package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.NotEnoughDoorsException;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.spigotutil.Abortable;
import nl.pim16aap2.bigdoors.spigotutil.PlayerRetriever;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.spigotutil.WorldRetriever;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.toolusers.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.TimedMapCache;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForAddOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForRemoveOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetBlocksToMove;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetTime;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager extends Restartable
{
    private final IStorage db;
    private final BigDoors plugin;
    private final Map<Long, Boolean> busyDoors;
    /**
     * Timed cache of all power blocks. The key is the hash of a chunk, the value a nested map.
     * <p>
     * The key of the nested map is the hash of a location (in world space) and the value of the nested map is the UID
     * of the door whose power block is stored in that location.
     */
    private final TimedMapCache<UUID, Map<Long /* Chunk */, Map<Long /* Loc */, List<Long> /* doorUIDs */>>> pbCache;
    // Players map stores players for faster UUID / Name matching.
    private boolean goOn = true;
    private boolean paused = false;

    public DatabaseManager(final BigDoors plugin, final String dbFile)
    {
        super(plugin);
        db = new SQLiteJDBCDriverConnection(new File(plugin.getDataFolder(), dbFile), plugin.getPLogger(),
                                            plugin.getConfigLoader(), new WorldRetriever(),
                                            new PlayerRetriever());

        pbCache = new TimedMapCache<>(plugin, Hashtable::new, plugin.getConfigLoader().cacheTimeout());
        this.plugin = plugin;
        busyDoors = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        busyDoors.clear();
    }

    public boolean isDoorBusy(long doorUID)
    {
        return busyDoors.containsKey(doorUID);
    }

    public void setDoorBusy(long doorUID)
    {
        busyDoors.put(doorUID, true);
    }

    public void setDoorAvailable(long doorUID)
    {
        busyDoors.remove(doorUID);
    }

    public void setDoorOpenTime(long doorUID, int autoClose)
    {
        updateDoorAutoClose(doorUID, autoClose);
    }

    private void emptyBusyDoors()
    {
        busyDoors.clear();
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
            playerUUID = db.getPlayerUUID(playerStr).orElse(null);
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
        startTimerForAbortable(
                new WaitForSetTime(plugin, (SubCommandSetAutoCloseTime) plugin.getCommand(CommandData.SETAUTOCLOSETIME),
                                   player, door), 20 * 20);
    }

    public void startBlocksToMoveSetter(Player player, DoorBase door)
    {
        startTimerForAbortable(new WaitForSetBlocksToMove(plugin, (SubCommandSetBlocksToMove) plugin
                .getCommand(CommandData.SETBLOCKSTOMOVE), player, door), 20 * 20);
    }

    public void startAddOwner(Player player, DoorBase door)
    {
        startTimerForAbortable(
                new WaitForAddOwner(plugin, (SubCommandAddOwner) plugin.getCommand(CommandData.ADDOWNER), player, door),
                20 * 20);
    }

    public void startRemoveOwner(Player player, DoorBase door)
    {
        startTimerForAbortable(
                new WaitForRemoveOwner(plugin, (SubCommandRemoveOwner) plugin.getCommand(CommandData.REMOVEOWNER),
                                       player, door), 20 * 20);
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

    public void addDoorBase(DoorBase newDoor)
    {
        db.insert(newDoor);
    }

    public void removeDoor(long doorUID)
    {
        db.removeDoor(doorUID);
    }

    // Returns a list of doors owner by a player and with a specific name, if provided (can be null).
    public Optional<List<DoorBase>> getDoors(@NotNull UUID playerUUID, @Nullable String name)
    {
        return name == null ? getDoors(playerUUID) : db.getDoors(playerUUID, name);
    }

    // Returns a List of doors owner by a player and with a specific name, if provided (can be null).
    public Optional<List<DoorBase>> getDoors(@NotNull UUID playerUUID)
    {
        return db.getDoors(playerUUID);
    }

    // Returns a List of doors owner by a player and with a specific name, if provided (can be null),
    // and where the player has a higher permission node (lower number) than specified.
    public Optional<List<DoorBase>> getDoors(@NotNull String playerUUID, @NotNull String name, int maxPermission)
    {
        return db.getDoors(playerUUID, name, maxPermission);
    }

    // Returns a List of doors with a specific name.
    public Optional<List<DoorBase>> getDoors(String name)
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

    public void updatePlayer(Player player)
    {
        db.updatePlayerName(player.getUniqueId().toString(), player.getName());
    }

    public Optional<DoorBase> getDoor(long doorUID)
    {
        return db.getDoor(doorUID);
    }

    // Get the door from the string. Can be use with a doorUID or a doorName.
    public Optional<DoorBase> getDoor(UUID playerUUID, String doorName)
            throws NotEnoughDoorsException, TooManyDoorsException
    {
        // First try converting the doorName to a doorUID.
        try
        {
            long doorUID = Long.parseLong(doorName);
            return db.getDoor(playerUUID, doorUID);
        }
        // If it can't convert to a long, get all doors from the player with the provided name.
        // If there is more than one, tell the player that they are going to have to make a choice.
        catch (NumberFormatException e)
        {
            if (playerUUID == null)
                return Optional.empty();
            int count = countDoorsOwnedByPlayer(playerUUID, doorName);
            if (count == 0)
                throw new NotEnoughDoorsException();
            if (count > 1)
                throw new TooManyDoorsException();
            return db.getDoor(playerUUID.toString(), doorName);
        }
    }

    // Get a door with a specific doorUID.
    public Optional<DoorBase> getDoor(UUID playerUUID, long doorUID)
    {
        return db.getDoor(playerUUID, doorUID);
    }

    public int countDoorsOwnedByPlayer(@NotNull UUID playerUUID)
    {
        return db.getDoorCountForPlayer(playerUUID);
    }

    public int countDoorsOwnedByPlayer(@NotNull UUID playerUUID, @NotNull String doorName)
    {
        return db.getDoorCountForPlayer(playerUUID, doorName);
    }

    public int countDoorsByName(@NotNull String doorName)
    {
        return db.getDoorCountByName(doorName);
    }

    public boolean hasPermissionForAction(Player player, long doorUID, DoorAttribute atr)
    {
        return hasPermissionForAction(player.getUniqueId(), doorUID, atr);
    }

    public boolean hasPermissionForAction(UUID playerUUID, long doorUID, DoorAttribute atr)
    {
        int playerPermission = getPermission(playerUUID.toString(), doorUID);
        return playerPermission >= 0 && playerPermission <= DoorAttribute.getPermissionLevel(atr);
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
                            blockYMax, blockZMax);
    }

    // Update the coordinates of a given door.
    public void updateDoorCoords(long doorUID, boolean isOpen, int blockXMin, int blockYMin,
                                 int blockZMin, int blockXMax, int blockYMax, int blockZMax,
                                 @Nullable PBlockFace newEngSide)
    {
        if (newEngSide == null)
            updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax, blockYMax, blockZMax);
        else
            db.updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax, blockYMax,
                                blockZMax, newEngSide);
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
        return db.removeOwner(doorUID, playerUUID.toString());
    }

    public List<DoorOwner> getDoorOwners(long doorUID)
    {
        return db.getOwnersOfDoor(doorUID);
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
    public @NotNull List<DoorBase> doorsFromPowerBlockLoc(@NotNull Location loc, @NotNull UUID worldUUID)
    {
        List<DoorBase> ret = new ArrayList<>();
        long chunkHash = Util.simpleChunkHashFromLocation(loc.getBlockX(), loc.getBlockZ());

        if (!pbCache.containsKey(worldUUID))
            pbCache.put(worldUUID, new HashMap<>());

        Map<Long, Map<Long, List<Long>>> worldMap = pbCache.get(worldUUID);

        Map<Long, List<Long>> powerBlockData;
        if (worldMap == null)
            plugin.getPLogger().logException(new IllegalStateException(), "worldMap is somehow null!??");
        if (worldMap.containsKey(chunkHash))
            powerBlockData = worldMap.get(chunkHash);
        else
        {
            powerBlockData = db.getPowerBlockData(chunkHash);
            worldMap.put(chunkHash, powerBlockData);
        }
        List<Long> doorUIDs = powerBlockData
                .get(Util.simpleLocationhash(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        doorUIDs.forEach(K -> db.getDoor(K).ifPresent(door -> ret.add(door)));
        return ret;
    }

    // Change the location of a powerblock.
    public void updatePowerBlockLoc(long doorUID, @NotNull Location loc)
    {
        Map<Long, Map<Long, List<Long>>> worldMap = pbCache.getOrDefault(loc.getWorld().getUID(), null);

        if (worldMap != null)
            // First, remove the chunk with the current power block location of this door from cache.
            db.getDoor(doorUID).ifPresent(door -> worldMap.remove(door.getSimplePowerBlockChunkHash()));
        db.updateDoorPowerBlockLoc(doorUID, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getUID());
        // Also make sure to remove the chunk of the new location of the power block from cache, so it gets
        // updated whenever its needed again.
        if (worldMap != null)
            worldMap.remove(Util.simpleChunkHashFromLocation(loc.getBlockX(), loc.getBlockZ()));
    }
}
