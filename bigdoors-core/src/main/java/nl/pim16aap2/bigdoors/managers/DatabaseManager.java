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
import nl.pim16aap2.bigdoors.spigotutil.AbortableTask;
import nl.pim16aap2.bigdoors.spigotutil.PlayerRetriever;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.spigotutil.WorldRetriever;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.toolusers.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages all database interactions.
 *
 * @author Pim
 */
public final class DatabaseManager extends Restartable
{
    private static DatabaseManager instance;

    private final IStorage db;
    private final BigDoors plugin;

    /**
     * Timed cache of all power blocks. The key is the {@link UUID} of the {@link org.bukkit.World}, the value is a
     * nested map.
     * <p>
     * The key of the first nested map if the hash of a chunk and yet another nested map.
     * <p>
     * The key of the second nested map is the hash of a location (in world space) and the value of the nested map is
     * the UID of the {@link DoorBase} whose power block is stored in that location.
     */
//    private final TimedMapCache<UUID /* World */, Map<Long /* Chunk */, Map<Long /* Loc */, List<Long> /* doorUIDs */>>> pbCache;
    private final Map<UUID /* World */, TimedMapCache<Long /* Chunk */, Map<Long /* Loc */, List<Long> /* doorUIDs */>>> pbCache;

    /**
     * Constructs a new {@link DatabaseManager}.
     *
     * @param plugin The spigot core.
     * @param dbFile The name of the database file.
     */
    private DatabaseManager(final @NotNull BigDoors plugin, final @NotNull String dbFile)
    {
        super(plugin);
        db = new SQLiteJDBCDriverConnection(new File(plugin.getDataFolder(), dbFile), plugin.getPLogger(),
                                            plugin.getConfigLoader(), new WorldRetriever(),
                                            new PlayerRetriever());
        this.plugin = plugin;
        pbCache = new HashMap<>();
    }

    /**
     * Initializes the {@link DatabaseManager}. If it has already been initialized, it'll return that instance instead.
     *
     * @param plugin The spigot core.
     * @param dbFile The name of the database file.
     * @return The instance of this {@link DatabaseManager}.
     */
    @NotNull
    public static DatabaseManager init(final @NotNull BigDoors plugin, final @NotNull String dbFile)
    {
        return (instance == null) ? instance = new DatabaseManager(plugin, dbFile) : instance;
    }

    /**
     * Gets the instance of the {@link DatabaseManager} if it exists.
     *
     * @return The instance of the {@link DatabaseManager}.
     */
    @Nullable
    public static DatabaseManager get()
    {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        pbCache.forEach((K, V) -> V.reInit(plugin.getConfigLoader().cacheTimeout()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        pbCache.forEach((K, V) -> V.shutdown());
    }

    /**
     * Changes the auto close time of a door.
     *
     * @param doorUID   The UID of the door.
     * @param autoClose The new auto close time.
     */
    public void setDoorOpenTime(final long doorUID, final int autoClose)
    {
        updateDoorAutoClose(doorUID, autoClose);
    }

    /**
     * Gets the {@link UUID} associated with a player name if exactly 1 exists.
     *
     * @param playerStr The name of the player.
     * @return The {@link UUID} associated with a player name
     */
    @NotNull
    public Optional<UUID> getPlayerUUIDFromString(final @NotNull String playerStr)
    {
        return Optional.ofNullable(SpigotUtil.playerUUIDFromString(playerStr)
                                             .orElse(db.getPlayerUUID(playerStr).orElse(null)));
    }

    /**
     * Starts the timer for an {@link AbortableTask}. The {@link AbortableTask} will be aborted after the provided
     * amount of time (in seconds).
     *
     * @param abortableTask The {@link AbortableTask}.
     * @param time          The amount of time (in seconds).
     */
    public void startTimerForAbortableTask(final @NotNull AbortableTask abortableTask, int time)
    {
        BukkitTask task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                abortableTask.abort(false);
            }
        }.runTaskLater(plugin, time);
        abortableTask.setTask(task);
    }

    /**
     * Starts the {@link PowerBlockRelocator} process for a given player and {@link DoorBase}.
     *
     * @param player The player.
     * @param door   The {@link DoorBase}.
     */
    public void startPowerBlockRelocator(final @NotNull Player player, final @NotNull DoorBase door)
    {
        startTimerForAbortableTask(new PowerBlockRelocator(plugin, player, door.getDoorUID()), 20 * 20);
    }

    /**
     * Starts the {@link WaitForSetTime} process for a given player and {@link DoorBase}.
     *
     * @param player The player.
     * @param door   The {@link DoorBase}.
     */
    public void startTimerSetter(final @NotNull Player player, final @NotNull DoorBase door)
    {
        startTimerForAbortableTask(
            new WaitForSetTime(plugin, (SubCommandSetAutoCloseTime) plugin.getCommand(CommandData.SETAUTOCLOSETIME),
                               player, door), 20 * 20);
    }

    /**
     * Starts the {@link WaitForSetBlocksToMove} process for a given player and {@link DoorBase}.
     *
     * @param player The player.
     * @param door   The {@link DoorBase}.
     */
    public void startBlocksToMoveSetter(final @NotNull Player player, final @NotNull DoorBase door)
    {
        startTimerForAbortableTask(new WaitForSetBlocksToMove(plugin, (SubCommandSetBlocksToMove) plugin
            .getCommand(CommandData.SETBLOCKSTOMOVE), player, door), 20 * 20);
    }

    /**
     * Starts the {@link WaitForAddOwner} process for a given player and {@link DoorBase}.
     *
     * @param player The player.
     * @param door   The {@link DoorBase}.
     */
    public void startAddOwner(final @NotNull Player player, final @NotNull DoorBase door)
    {
        startTimerForAbortableTask(
            new WaitForAddOwner(plugin, (SubCommandAddOwner) plugin.getCommand(CommandData.ADDOWNER), player, door),
            20 * 20);
    }

    /**
     * Starts the {@link WaitForRemoveOwner} process for a given player and {@link DoorBase}.
     *
     * @param player The player.
     * @param door   The {@link DoorBase}.
     */
    public void startRemoveOwner(final @NotNull Player player, final @NotNull DoorBase door)
    {
        startTimerForAbortableTask(
            new WaitForRemoveOwner(plugin, (SubCommandRemoveOwner) plugin.getCommand(CommandData.REMOVEOWNER),
                                   player, door), 20 * 20);
    }

    /**
     * Changes the number of blocks a {@link DoorBase} will try to move.
     *
     * @param doorUID      The UID of a {@link DoorBase}.
     * @param blocksToMove The number of blocks the {@link DoorBase} will try to move.
     */
    public void setDoorBlocksToMove(final long doorUID, final int blocksToMove)
    {
        plugin.getDatabaseManager().updateDoorBlocksToMove(doorUID, blocksToMove);
    }

    /**
     * Inserts a {@link DoorBase} into the database.
     *
     * @param newDoor The new {@link DoorBase}.
     */
    public void addDoorBase(final @NotNull DoorBase newDoor)
    {
        db.insert(newDoor);
    }

    /**
     * Removes a {@link DoorBase} from the database.
     *
     * @param doorUID The UID of the door.
     */
    public void removeDoor(final long doorUID)
    {
        db.removeDoor(doorUID);
    }

    /**
     * Gets all {@link DoorBase} owned by a player. Only searches for {@link DoorBase} with a given name if one was
     * provided.
     *
     * @param playerUUID The {@link UUID} of the payer.
     * @param name       The name of the {@link DoorBase} to search for. Can be null.
     * @return All {@link DoorBase} owned by a player with a specific name.
     */
    @NotNull
    public Optional<List<DoorBase>> getDoors(final @NotNull UUID playerUUID, final @Nullable String name)
    {
        return name == null ? getDoors(playerUUID) : db.getDoors(playerUUID, name);
    }

    /**
     * Gets all {@link DoorBase} owned by a player.
     *
     * @param playerUUID The {@link UUID} of the payer.
     * @return All {@link DoorBase} owned by a player.
     */
    @NotNull
    public Optional<List<DoorBase>> getDoors(final @NotNull UUID playerUUID)
    {
        return db.getDoors(playerUUID);
    }

    /**
     * Gets all {@link DoorBase} owned by a player with a specific name.
     *
     * @param playerUUID    The {@link UUID} of the payer.
     * @param name          The name of the {@link DoorBase} to search for.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the {@link DoorBase}s.
     * @return All {@link DoorBase} owned by a player with a specific name.
     */
    @NotNull
    public Optional<List<DoorBase>> getDoors(final @NotNull String playerUUID, final @NotNull String name,
                                             final int maxPermission)
    {
        return db.getDoors(playerUUID, name, maxPermission);
    }

    /**
     * Gets all {@link DoorBase}s with a specific name, regardless over ownership.
     *
     * @param name The name of the {@link DoorBase}s.
     * @return All {@link DoorBase}s with a specific name.
     */
    @NotNull
    public Optional<List<DoorBase>> getDoors(final @NotNull String name)
    {
        return db.getDoors(name);
    }

    /**
     * Replaces all blocks between the minimum and maximum coordinates of a {@link DoorBase} with stone.
     *
     * @param door The {@link DoorBase}.
     */
    public void fillDoor(final @NotNull DoorBase door)
    {
        for (int i = door.getMinimum().getBlockX(); i <= door.getMaximum().getBlockX(); ++i)
            for (int j = door.getMinimum().getBlockY(); j <= door.getMaximum().getBlockY(); ++j)
                for (int k = door.getMinimum().getBlockZ(); k <= door.getMaximum().getBlockZ(); ++k)
                    door.getWorld().getBlockAt(i, j, k).setType(Material.STONE);
    }

    /**
     * Updates the name of a player in the database, to make sure the player's name and UUID don't go out of sync.
     *
     * @param player The Player.
     */
    public void updatePlayer(final @NotNull Player player)
    {
        db.updatePlayerName(player.getUniqueId().toString(), player.getName());
    }

    /**
     * Gets the {@link DoorBase} with a specific UID.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @return The {@link DoorBase} if it exists.
     */
    @NotNull
    public Optional<DoorBase> getDoor(final long doorUID)
    {
        return db.getDoor(doorUID);
    }

    /**
     * Gets the {@link DoorBase} with a specific name owned by a specific player if exactly one such {@link DoorBase}
     * exists.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @param doorName   The name of the {@link DoorBase}.
     * @return The {@link DoorBase} with a specific name owned by a specific player if exactly one such {@link DoorBase}
     * exists.
     *
     * @throws NotEnoughDoorsException If no {@link DoorBase} meeting the criteria were found.
     * @throws TooManyDoorsException   If more than 1 {@link DoorBase} meeting the criteria were found.
     */
    @NotNull
    public Optional<DoorBase> getDoor(final @NotNull UUID playerUUID, final @NotNull String doorName)
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
            int count = countDoorsOwnedByPlayer(playerUUID, doorName);
            if (count == 0)
                throw new NotEnoughDoorsException();
            if (count > 1)
                throw new TooManyDoorsException();
            return db.getDoor(playerUUID.toString(), doorName);
        }
    }

    /**
     * Gets the {@link DoorBase} with the given UID owned by the player, if provided. Otherwise, the original creator is
     * used as {@link DoorOwner}.
     *
     * @param playerUUID The {@link UUID} of the player. Null will default to the original creator.
     * @param doorUID    The UID of the {@link DoorBase}.
     * @return The {@link DoorBase} with the given UID owned by the player, if provided.
     */
    @NotNull
    public Optional<DoorBase> getDoor(final @Nullable UUID playerUUID, final long doorUID)
    {
        return playerUUID == null ? db.getDoor(doorUID) : db.getDoor(playerUUID, doorUID);
    }

    /**
     * Gets the number of {@link DoorBase}s owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @return The number of {@link DoorBase}s this player owns.
     */
    public int countDoorsOwnedByPlayer(final @NotNull UUID playerUUID)
    {
        return db.getDoorCountForPlayer(playerUUID);
    }

    /**
     * Counts the number of {@link DoorBase}s with a specific name owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @param doorName   The name of the door.
     * @return The number of {@link DoorBase}s with a specific name owned by a player.
     */
    public int countDoorsOwnedByPlayer(final @NotNull UUID playerUUID, final @NotNull String doorName)
    {
        return db.getDoorCountForPlayer(playerUUID, doorName);
    }

    public int countOwnersOfDoor(final long doorUID)
    {
        return db.getOwnerCountOfDoor(doorUID);
    }

    /**
     * The number of {@link DoorBase}s in the database with a specific name.
     *
     * @param doorName The name of the {@link DoorBase}.
     * @return The number of {@link DoorBase}s with a specific name.
     */
    public int countDoorsByName(final @NotNull String doorName)
    {
        return db.getDoorCountByName(doorName);
    }

    /**
     * Checks if a player has a high enough lever of ownership over a {@link DoorBase} to interact with a specific
     * {@link DoorAttribute}.
     *
     * @param player  The {@link Player}.
     * @param doorUID The UID of the {@link DoorBase}.
     * @param atr     The {@link DoorAttribute}.
     * @return True if the player has a high enough lever of ownership over a {@link DoorBase} to interact with a
     * specific {@link DoorAttribute}.
     */
    public boolean hasPermissionForAction(final @NotNull Player player, final long doorUID,
                                          final @NotNull DoorAttribute atr)
    {
        return hasPermissionForAction(player.getUniqueId(), doorUID, atr);
    }

    /**
     * Checks if a player has a high enough lever of ownership over a {@link DoorBase} to interact with a specific
     * {@link DoorAttribute}.
     *
     * @param playerUUID The {@link UUID} of the {@link Player}.
     * @param doorUID    The UID of the {@link DoorBase}.
     * @param atr        The {@link DoorAttribute}.
     * @return True if the player has a high enough lever of ownership over a {@link DoorBase} to interact with a
     * specific {@link DoorAttribute}.
     */
    public boolean hasPermissionForAction(final @NotNull UUID playerUUID, final long doorUID,
                                          final @NotNull DoorAttribute atr)
    {
        int playerPermission = getPermission(playerUUID.toString(), doorUID);
        return playerPermission >= 0 && playerPermission <= DoorAttribute.getPermissionLevel(atr);
    }

    /**
     * Gets the level of ownership a player has over a {@link DoorBase}.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @param doorUID    The UID of the {@link DoorBase}.
     * @return The level of ownership a player has over a {@link DoorBase}.
     */
    public int getPermission(final @NotNull String playerUUID, final long doorUID)
    {
        return db.getPermission(playerUUID, doorUID);
    }

    /**
     * Updates the coordinates of a {@link DoorBase} in the database.
     *
     * @param doorUID   The UID of the {@link DoorBase}.
     * @param isOpen    Whether the {@link DoorBase} is now open or not.
     * @param blockXMin The lower bound x coordinates.
     * @param blockYMin The lower bound y coordinates.
     * @param blockZMin The lower bound z coordinates.
     * @param blockXMax The upper bound x coordinates.
     * @param blockYMax The upper bound y coordinates.
     * @param blockZMax The upper bound z coordinates.
     */
    public void updateDoorCoords(final long doorUID, final boolean isOpen, final int blockXMin, final int blockYMin,
                                 final int blockZMin, final int blockXMax, final int blockYMax, final int blockZMax)
    {
        db.updateDoorCoords(doorUID, isOpen, blockXMin, blockYMin, blockZMin, blockXMax,
                            blockYMax, blockZMax);
    }

    /**
     * Adds a player as owner to a {@link DoorBase} at a given level of ownership.
     *
     * @param door       The {@link DoorBase}.
     * @param playerUUID The {@link UUID} of the {@link Player}.
     * @param permission The level of ownership.
     * @return True if owner addition was successful.
     */
    public boolean addOwner(final @NotNull DoorBase door, final @NotNull UUID playerUUID, final int permission)
    {
        if (permission < 1 || permission > 2 || door.getPermission() != 0 || door.getPlayerUUID().equals(playerUUID))
            return false;

        db.addOwner(door.getDoorUID(), playerUUID, permission);
        return true;
    }

    /**
     * Remove a {@link Player} as owner of a {@link DoorBase}.
     *
     * @param door       The {@link DoorBase}.
     * @param playerUUID The {@link UUID} of the {@link Player}.
     * @return True if owner removal was successful.
     */
    public boolean removeOwner(final @NotNull DoorBase door, final @NotNull UUID playerUUID)
    {
        return removeOwner(door.getDoorUID(), playerUUID);
    }

    /**
     * Remove a {@link Player} as owner of a {@link DoorBase}.
     *
     * @param doorUID    The UID of the {@link DoorBase}.
     * @param playerUUID The {@link UUID} of the {@link Player}.
     * @return True if owner removal was successful.
     */
    public boolean removeOwner(final long doorUID, final @NotNull UUID playerUUID)
    {
        if (db.getPermission(playerUUID.toString(), doorUID) == 0)
            return false;
        return db.removeOwner(doorUID, playerUUID.toString());
    }

    /**
     * Gets all owners of a {@link DoorBase}.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @return All owners of a {@link DoorBase}.
     */
    public List<DoorOwner> getDoorOwners(final long doorUID)
    {
        return db.getOwnersOfDoor(doorUID);
    }

    /**
     * Updates the opening direction of a {@link DoorBase}.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @param openDir The new opening direction.
     */
    public void updateDoorOpenDirection(final long doorUID, final @NotNull RotateDirection openDir)
    {
        db.updateDoorOpenDirection(doorUID, openDir);
    }

    /**
     * Updates the auto close timer of a {@link DoorBase}.
     *
     * @param doorUID   The UID of the {@link DoorBase}.
     * @param autoClose The new auto close timer value.
     */
    public void updateDoorAutoClose(final long doorUID, final int autoClose)
    {
        db.updateDoorAutoClose(doorUID, autoClose);
    }

    /**
     * Updates the number of blocks a {@link DoorBase} will try to move.
     *
     * @param doorUID      The UID of the {@link DoorBase}.
     * @param blocksToMove The new number of blocks to move value.
     */
    public void updateDoorBlocksToMove(final long doorUID, final int blocksToMove)
    {
        db.updateDoorBlocksToMove(doorUID, blocksToMove);
    }

    /**
     * Changes the locked status of a {@link DoorBase}.
     *
     * @param doorUID       The UID of the {@link DoorBase}.
     * @param newLockStatus The new locked status.
     */
    public void setLock(final long doorUID, final boolean newLockStatus)
    {
        db.setLock(doorUID, newLockStatus);
    }

    /**
     * Gets all {@link DoorBase}s that have a powerblock at a location in a world.
     *
     * @param loc       The location.
     * @param worldUUID The {@link UUID} of the world.
     * @return All {@link DoorBase}s that have a powerblock at a location in a world.
     */
    @NotNull
    public List<DoorBase> doorsFromPowerBlockLoc(final @NotNull Location loc, final @NotNull UUID worldUUID)
    {
        List<DoorBase> ret = new ArrayList<>();
        long chunkHash = Util.simpleChunkHashFromLocation(loc.getBlockX(), loc.getBlockZ());

        if (!pbCache.containsKey(worldUUID))
            pbCache.put(worldUUID, new TimedMapCache<>(plugin, HashMap::new, plugin.getConfigLoader().cacheTimeout()));

        TimedMapCache<Long, Map<Long, List<Long>>> worldMap = pbCache.get(worldUUID);

        Map<Long, List<Long>> powerBlockData = worldMap.get(chunkHash);
        if (powerBlockData == null)
        {
            powerBlockData = db.getPowerBlockData(chunkHash);
            worldMap.put(chunkHash, powerBlockData);
        }
        List<Long> doorUIDs = powerBlockData
            .getOrDefault(Util.simpleLocationhash(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                          new ArrayList<>());
        doorUIDs.forEach(K -> db.getDoor(K).ifPresent(ret::add));
        return ret;
    }

    /**
     * Updates the Location of the powerblock of a {@link DoorBase} in the database.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @param loc     The new Location.
     */
    public void updatePowerBlockLoc(final long doorUID, final @NotNull Location loc)
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
