package nl.pim16aap2.bigdoors.managers;

import com.google.common.base.Preconditions;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.doors.DoorBase;
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
import nl.pim16aap2.bigdoors.util.Vector3D;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForAddOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForRemoveOwner;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetBlocksToMove;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForSetTime;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages all database interactions.
 *
 * @author Pim
 */
public final class DatabaseManager extends Restartable
{
    @Nullable
    private static DatabaseManager instance;

    /**
     * The thread pool to use for storage access.
     */
    private final ExecutorService threadPool;

    /**
     * The number of threads to use for storage access if the storage allows multithreaded access as determined by
     * {@link IStorage#isSingleThreaded()}.
     */
    private static final int THREADCOUNT = 10;

    private final IStorage db;
    private final BigDoors plugin;

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
        if (db.isSingleThreaded())
            threadPool = Executors.newSingleThreadExecutor();
        else
            threadPool = Executors.newFixedThreadPool(THREADCOUNT);
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
    @NotNull
    public static DatabaseManager get()
    {
        Preconditions.checkState(instance != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
    }

    /**
     * Changes the auto close time of a door.
     *
     * @param doorUID   The UID of the door.
     * @param autoClose The new auto close time.
     */
    public void setDoorOpenTime(final long doorUID, final int autoClose)
    {
        CompletableFuture.runAsync(() -> updateDoorAutoClose(doorUID, autoClose), threadPool);
    }

    /**
     * Gets the {@link UUID} associated with a player name if exactly 1 exists.
     *
     * @param playerStr The name of the player.
     * @return The {@link UUID} associated with a player name
     */
    @NotNull
    public CompletableFuture<Optional<UUID>> getPlayerUUIDFromString(final @NotNull String playerStr)
    {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(SpigotUtil.playerUUIDFromString(playerStr)
                                                .orElse(db.getPlayerUUID(playerStr).orElse(null))), threadPool);
    }

    /**
     * Changes the number of blocks a {@link DoorBase} will try to move.
     *
     * @param doorUID      The UID of a {@link DoorBase}.
     * @param blocksToMove The number of blocks the {@link DoorBase} will try to move.
     */
    public void setDoorBlocksToMove(final long doorUID, final int blocksToMove)
    {
        CompletableFuture.runAsync(() -> updateDoorBlocksToMove(doorUID, blocksToMove), threadPool);
    }

    /**
     * Inserts a {@link DoorBase} into the database.
     *
     * @param newDoor The new {@link DoorBase}.
     */
    public void addDoorBase(final @NotNull DoorBase newDoor)
    {
        CompletableFuture.runAsync(
            () ->
            {
                db.insert(newDoor);
                plugin.getPowerBlockManager().onDoorAddOrRemove(newDoor.getWorld().getUID(),
                                                                new Vector3D(newDoor.getPowerBlockLoc().getBlockX(),
                                                                             newDoor.getPowerBlockLoc().getBlockY(),
                                                                             newDoor.getPowerBlockLoc().getBlockZ()));
            }, threadPool);
    }

    /**
     * Removes a {@link DoorBase} from the database.
     *
     * @param door The door.
     */
    public void removeDoor(final @NotNull DoorBase door)
    {
        CompletableFuture.runAsync(
            () ->
            {
                db.removeDoor(door.getDoorUID());
                plugin.getPowerBlockManager().onDoorAddOrRemove(door.getWorld().getUID(),
                                                                new Vector3D(door.getPowerBlockLoc().getBlockX(),
                                                                             door.getPowerBlockLoc().getBlockY(),
                                                                             door.getPowerBlockLoc().getBlockZ()));
            }, threadPool);
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
    public CompletableFuture<Optional<List<DoorBase>>> getDoors(final @NotNull UUID playerUUID,
                                                                final @Nullable String name)
    {
        return name == null ? getDoors(playerUUID) :
               CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID, name), threadPool);
    }

    /**
     * Gets all {@link DoorBase} owned by a player.
     *
     * @param playerUUID The {@link UUID} of the payer.
     * @return All {@link DoorBase} owned by a player.
     */
    @NotNull
    public CompletableFuture<Optional<List<DoorBase>>> getDoors(final @NotNull UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID), threadPool);
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
    public CompletableFuture<Optional<List<DoorBase>>> getDoors(final @NotNull String playerUUID,
                                                                final @NotNull String name,
                                                                final int maxPermission)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID, name, maxPermission), threadPool);
    }

    /**
     * Gets all {@link DoorBase}s with a specific name, regardless over ownership.
     *
     * @param name The name of the {@link DoorBase}s.
     * @return All {@link DoorBase}s with a specific name.
     */
    @NotNull
    public CompletableFuture<Optional<List<DoorBase>>> getDoors(final @NotNull String name)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(name), threadPool);
    }

    /**
     * Updates the name of a player in the database, to make sure the player's name and UUID don't go out of sync.
     *
     * @param player The Player.
     */
    public void updatePlayer(final @NotNull Player player)
    {
        CompletableFuture
            .supplyAsync(() -> db.updatePlayerName(player.getUniqueId().toString(), player.getName()), threadPool);
    }

    /**
     * Gets the {@link DoorBase} with a specific UID.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @return The {@link DoorBase} if it exists.
     */
    @NotNull
    public CompletableFuture<Optional<DoorBase>> getDoor(final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoor(doorUID), threadPool);
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
    public CompletableFuture<Optional<DoorBase>> getDoor(final @Nullable UUID playerUUID, final long doorUID)
    {
        return playerUUID == null ?
               CompletableFuture.supplyAsync(() -> db.getDoor(doorUID), threadPool) :
               CompletableFuture.supplyAsync(() -> db.getDoor(playerUUID, doorUID), threadPool);
    }

    /**
     * Gets the number of {@link DoorBase}s owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @return The number of {@link DoorBase}s this player owns.
     */
    public CompletableFuture<Integer> countDoorsOwnedByPlayer(final @NotNull UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID), threadPool);
    }

    /**
     * Counts the number of {@link DoorBase}s with a specific name owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @param doorName   The name of the door.
     * @return The number of {@link DoorBase}s with a specific name owned by a player.
     */
    public CompletableFuture<Integer> countDoorsOwnedByPlayer(final @NotNull UUID playerUUID,
                                                              final @NotNull String doorName)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID, doorName), threadPool);
    }

    public CompletableFuture<Integer> countOwnersOfDoor(final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getOwnerCountOfDoor(doorUID), threadPool);
    }

    /**
     * The number of {@link DoorBase}s in the database with a specific name.
     *
     * @param doorName The name of the {@link DoorBase}.
     * @return The number of {@link DoorBase}s with a specific name.
     */
    public CompletableFuture<Integer> countDoorsByName(final @NotNull String doorName)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountByName(doorName), threadPool);
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
    public CompletableFuture<Boolean> hasPermissionForAction(final @NotNull Player player, final long doorUID,
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
    public CompletableFuture<Boolean> hasPermissionForAction(final @NotNull UUID playerUUID, final long doorUID,
                                                             final @NotNull DoorAttribute atr)
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                int playerPermission;
                try
                {
                    playerPermission = getPermission(playerUUID.toString(), doorUID).get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    plugin.getPLogger().logException(e);
                    playerPermission = Integer.MAX_VALUE;
                }
                return playerPermission >= 0 && playerPermission <= DoorAttribute.getPermissionLevel(atr);
            });
    }

    /**
     * Gets the level of ownership a player has over a {@link DoorBase}.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @param doorUID    The UID of the {@link DoorBase}.
     * @return The level of ownership a player has over a {@link DoorBase}.
     */
    public CompletableFuture<Integer> getPermission(final @NotNull String playerUUID, final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getPermission(playerUUID, doorUID), threadPool);
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
        CompletableFuture.runAsync(() -> db.updateDoorCoords(doorUID, isOpen,
                                                             blockXMin, blockYMin, blockZMin,
                                                             blockXMax, blockYMax, blockZMax), threadPool);
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

        CompletableFuture.runAsync(() -> db.addOwner(door.getDoorUID(), playerUUID, permission), threadPool);
        return true;
    }

    /**
     * Remove a {@link Player} as owner of a {@link DoorBase}.
     *
     * @param door       The {@link DoorBase}.
     * @param playerUUID The {@link UUID} of the {@link Player}.
     * @return True if owner removal was successful.
     */
    public CompletableFuture<Boolean> removeOwner(final @NotNull DoorBase door, final @NotNull UUID playerUUID)
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
    public CompletableFuture<Boolean> removeOwner(final long doorUID, final @NotNull UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                if (db.getPermission(playerUUID.toString(), doorUID) == 0)
                    return false;
                return db.removeOwner(doorUID, playerUUID.toString());
            }, threadPool);
    }

    /**
     * Gets all owners of a {@link DoorBase}.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @return All owners of a {@link DoorBase}.
     */
    public CompletableFuture<List<DoorOwner>> getDoorOwners(final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getOwnersOfDoor(doorUID), threadPool);
    }

    /**
     * Updates the opening direction of a {@link DoorBase}.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @param openDir The new opening direction.
     */
    public void updateDoorOpenDirection(final long doorUID, final @NotNull RotateDirection openDir)
    {
        CompletableFuture.runAsync(() -> db.updateDoorOpenDirection(doorUID, openDir), threadPool);
    }

    /**
     * Updates the auto close timer of a {@link DoorBase}.
     *
     * @param doorUID   The UID of the {@link DoorBase}.
     * @param autoClose The new auto close timer value.
     */
    public void updateDoorAutoClose(final long doorUID, final int autoClose)
    {
        CompletableFuture.runAsync(() -> db.updateDoorAutoClose(doorUID, autoClose), threadPool);
    }

    /**
     * Updates the number of blocks a {@link DoorBase} will try to move.
     *
     * @param doorUID      The UID of the {@link DoorBase}.
     * @param blocksToMove The new number of blocks to move value.
     */
    public void updateDoorBlocksToMove(final long doorUID, final int blocksToMove)
    {
        CompletableFuture.runAsync(() -> db.updateDoorBlocksToMove(doorUID, blocksToMove), threadPool);
    }

    /**
     * Changes the locked status of a {@link DoorBase}.
     *
     * @param doorUID       The UID of the {@link DoorBase}.
     * @param newLockStatus The new locked status.
     */
    public void setLock(final long doorUID, final boolean newLockStatus)
    {
        CompletableFuture.runAsync(() -> db.setLock(doorUID, newLockStatus), threadPool);
    }

    /**
     * Updates the location of a power block of a door.
     *
     * @param doorUID   The UID of the door.
     * @param newLoc    The new location.
     * @param worldUUID The UUID of the world.
     */
    void updatePowerBlockLoc(final long doorUID, final @NotNull Vector3D newLoc, final @NotNull UUID worldUUID)
    {
        CompletableFuture.runAsync(() -> db.updateDoorPowerBlockLoc(doorUID, newLoc.getX(), newLoc.getY(),
                                                                    newLoc.getZ(), worldUUID), threadPool);
    }

    /**
     * Checks if a world contains any big doors.
     *
     * @param world The world.
     * @return True if at least 1 door exists in the world.
     */
    CompletableFuture<Boolean> isBigDoorsWorld(final @NotNull UUID world)
    {
        return CompletableFuture.supplyAsync(() -> db.isBigDoorsWorld(world), threadPool);
    }

    /**
     * Gets a map of location hashes and their connected powerblocks for all doors in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the doors whose powerblocks
     * occupies that location.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A map of location hashes and their connected powerblocks for all doors in a chunk.
     */
    @NotNull
    CompletableFuture<ConcurrentHashMap<Integer, List<Long>>> getPowerBlockData(final long chunkHash)
    {
        return CompletableFuture.supplyAsync(() -> db.getPowerBlockData(chunkHash), threadPool);
    }

    /*






    THIS SHOULD BE PUT INTO ITS OWN CLASS!!!!!! IT HAS NO PURPOSE HERE!

     */

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
        startTimerForAbortableTask(new PowerBlockRelocator(plugin, player, door.getDoorUID(), door.getPowerBlockLoc()),
                                   20 * 20);
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
}
