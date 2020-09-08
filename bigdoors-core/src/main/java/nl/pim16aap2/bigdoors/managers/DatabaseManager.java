package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Pair;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

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
    @NotNull
    private final ExecutorService threadPool;

    /**
     * The number of threads to use for storage access if the storage allows multithreaded access as determined by
     * {@link IStorage#isSingleThreaded()}.
     */
    private static final int THREADCOUNT = 10;

    private final IStorage db;

    /**
     * Constructs a new {@link DatabaseManager}.
     *
     * @param restartableHolder The object managing restarts for this object.
     * @param dbFile            The name of the database file.
     */
    private DatabaseManager(final @NotNull IRestartableHolder restartableHolder, final @NotNull File dbFile)
    {
        super(restartableHolder);
        db = new SQLiteJDBCDriverConnection(dbFile);
        if (db.isSingleThreaded())
            threadPool = Executors.newSingleThreadExecutor();
        else
            threadPool = Executors.newFixedThreadPool(THREADCOUNT);
    }

    /**
     * Initializes the {@link DatabaseManager}. If it has already been initialized, it'll return that instance instead.
     *
     * @param restartableHolder The object managing restarts for this object.
     * @param dbFile            The name of the database file.
     * @return The instance of this {@link DatabaseManager}.
     */
    public static @NotNull DatabaseManager init(final @NotNull IRestartableHolder restartableHolder,
                                                final @NotNull File dbFile)
    {
        return (instance == null) ? instance = new DatabaseManager(restartableHolder, dbFile) : instance;
    }

    /**
     * Registeres an {@link DoorType} in the database.
     *
     * @param doorType The {@link DoorType}.
     * @return The identifier value assigned to the {@link DoorType} during registration. A value less than 1 means that
     * registration was not successful. If the {@link DoorType} already exists in the database, it will return the
     * existing identifier value. As long as the type does not change,
     */
    public @NotNull CompletableFuture<Long> registerDoorType(final @NotNull DoorType doorType)
    {
        return CompletableFuture.supplyAsync(() -> db.registerDoorType(doorType));
    }

    /**
     * Gets the instance of the {@link DatabaseManager} if it exists.
     *
     * @return The instance of the {@link DatabaseManager}.
     */
    public static @NotNull DatabaseManager get()
    {
//        Preconditions.checkState(instance != null,
//                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return instance;
    }

    /**
     * Obtains {@link IStorage.DatabaseState} the database is in.
     *
     * @return The {@link IStorage.DatabaseState} the database is in.
     */
    public @NotNull IStorage.DatabaseState getDatabaseState()
    {
        return db.getDatabaseState();
    }

    @Override
    public void restart()
    {
    }

    @Override
    public void shutdown()
    {
    }

    /**
     * Inserts a {@link AbstractDoorBase} into the database.
     *
     * @param newDoor The new {@link AbstractDoorBase}.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> addDoorBase(final @NotNull AbstractDoorBase newDoor)
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                final @NotNull Optional<AbstractDoorBase> result = db.insert(newDoor);
                result.ifPresent(
                    (door) -> BigDoors.get().getPowerBlockManager()
                                      .onDoorAddOrRemove(door.getWorld().getUUID(), new Vector3Di(
                                          door.getPowerBlock().getX(),
                                          door.getPowerBlock().getY(),
                                          door.getPowerBlock().getZ())));
                return result;
            }, threadPool);
    }

    /**
     * Removes a {@link AbstractDoorBase} from the database.
     *
     * @param door The door.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> deleteDoor(final @NotNull AbstractDoorBase door)
    {
        DoorRegistry.get().deregisterDoor(door.getDoorUID());
        return CompletableFuture.supplyAsync(
            () ->
            {
                boolean result = db.removeDoor(door.getDoorUID());
                if (result)
                    BigDoors.get().getPowerBlockManager().onDoorAddOrRemove(door.getWorld().getUUID(), new Vector3Di(
                        door.getPowerBlock().getX(),
                        door.getPowerBlock().getY(),
                        door.getPowerBlock().getZ()));
                return result;
            }, threadPool);
    }

    /**
     * Gets a list of door UIDs that have their engine in a given chunk.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A list of door UIDs that have their engine in a given chunk.
     */
    public @NotNull CompletableFuture<List<Long>> getDoorsInChunk(final long chunkHash)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorsInChunk(chunkHash), threadPool);
    }

    /**
     * Gets all {@link AbstractDoorBase} owned by a player. Only searches for {@link AbstractDoorBase} with a given name
     * if one was provided.
     *
     * @param playerUUID The {@link UUID} of the payer.
     * @param name       The name or the UID of the {@link AbstractDoorBase} to search for. Can be null.
     * @return All {@link AbstractDoorBase} owned by a player with a specific name.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID,
                                                                       final @Nullable String name)
    {
        // Check if the name is actually the UID of the door.
        final @NotNull Pair<Boolean, Long> doorID = Util.longFromString(name);
        if (doorID.first)
            return CompletableFuture
                .supplyAsync(() -> db.getDoor(playerUUID, doorID.second)
                                     .map(Collections::singletonList)
                                     .orElse(Collections.emptyList()), threadPool);


        return name == null ? getDoors(playerUUID) :
               CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID, name), threadPool);

    }

    /**
     * Gets all {@link AbstractDoorBase} owned by a player.
     *
     * @param playerUUID The {@link UUID} of the payer.
     * @return All {@link AbstractDoorBase} owned by a player.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID), threadPool);
    }

    /**
     * Gets all {@link AbstractDoorBase} owned by a player with a specific name.
     *
     * @param playerUUID    The {@link UUID} of the payer.
     * @param name          The name of the {@link AbstractDoorBase} to search for.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the {@link
     *                      AbstractDoorBase}s.
     * @return All {@link AbstractDoorBase} owned by a player with a specific name.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull String playerUUID,
                                                                       final @NotNull String name,
                                                                       final int maxPermission)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID, name, maxPermission), threadPool);
    }

    /**
     * Gets all {@link AbstractDoorBase}s with a specific name, regardless over ownership.
     *
     * @param name The name of the {@link AbstractDoorBase}s.
     * @return All {@link AbstractDoorBase}s with a specific name.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull String name)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(name), threadPool);
    }

    /**
     * Updates the name of a player in the database, to make sure the player's name and UUID don't go out of sync.
     *
     * @param player The Player.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> updatePlayer(final @NotNull IPPlayer player)
    {
        return CompletableFuture
            .supplyAsync(() -> db.updatePlayerName(player.getUUID().toString(), player.getName()), threadPool);
    }

    /**
     * Gets the {@link AbstractDoorBase} with a specific UID.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The {@link AbstractDoorBase} if it exists.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoor(doorUID), threadPool);
    }

    /**
     * Gets the {@link AbstractDoorBase} with the given UID owned by the player, if provided. Otherwise, the original
     * creator is used as {@link DoorOwner}.
     *
     * @param player  The player. Null will default to the original creator.
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The {@link AbstractDoorBase} with the given UID owned by the player, if provided.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @Nullable IPPlayer player,
                                                                          final long doorUID)
    {
        return player == null ?
               CompletableFuture.supplyAsync(() -> db.getDoor(doorUID), threadPool) :
               CompletableFuture.supplyAsync(() -> db.getDoor(player.getUUID(), doorUID),
                                             threadPool);
    }

    /**
     * Gets the number of {@link AbstractDoorBase}s owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @return The number of {@link AbstractDoorBase}s this player owns.
     */
    public @NotNull CompletableFuture<Integer> countDoorsOwnedByPlayer(final @NotNull UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID), threadPool);
    }

    /**
     * Counts the number of {@link AbstractDoorBase}s with a specific name owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @param doorName   The name of the door.
     * @return The number of {@link AbstractDoorBase}s with a specific name owned by a player.
     */
    public @NotNull CompletableFuture<Integer> countDoorsOwnedByPlayer(final @NotNull UUID playerUUID,
                                                                       final @NotNull String doorName)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID, doorName), threadPool);
    }

    /**
     * The number of {@link AbstractDoorBase}s in the database with a specific name.
     *
     * @param doorName The name of the {@link AbstractDoorBase}.
     * @return The number of {@link AbstractDoorBase}s with a specific name.
     */
    public @NotNull CompletableFuture<Integer> countDoorsByName(final @NotNull String doorName)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountByName(doorName), threadPool);
    }

    /**
     * Checks if a player has a high enough lever of ownership over a {@link AbstractDoorBase} to interact with a
     * specific {@link DoorAttribute}.
     *
     * @param player  The {@link IPPlayer}.
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @param atr     The {@link DoorAttribute}.
     * @return True if the player has a high enough lever of ownership over a {@link AbstractDoorBase} to interact with
     * a specific {@link DoorAttribute}.
     */
    public @NotNull CompletableFuture<Boolean> hasPermissionForAction(final @NotNull IPPlayer player,
                                                                      final long doorUID,
                                                                      final @NotNull DoorAttribute atr)
    {
        return hasPermissionForAction(player.getUUID(), doorUID, atr);
    }

    /**
     * Checks if a player has a high enough lever of ownership over a {@link AbstractDoorBase} to interact with a
     * specific {@link DoorAttribute}.
     *
     * @param playerUUID The {@link UUID} of the {@link IPPlayer}.
     * @param doorUID    The UID of the {@link AbstractDoorBase}.
     * @param atr        The {@link DoorAttribute}.
     * @return True if the player has a high enough lever of ownership over a {@link AbstractDoorBase} to interact with
     * a specific {@link DoorAttribute}.
     */
    public @NotNull CompletableFuture<Boolean> hasPermissionForAction(final @NotNull UUID playerUUID,
                                                                      final long doorUID,
                                                                      final @NotNull DoorAttribute atr)
    {
        // TODO: Use the door itself for this.
        return CompletableFuture.supplyAsync(
            () ->
            {
                int playerPermission;
                try
                {
                    playerPermission = getPermission(playerUUID, doorUID).get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    PLogger.get().logThrowable(e);
                    playerPermission = Integer.MAX_VALUE;
                }
                return playerPermission >= 0 && playerPermission <= DoorAttribute.getPermissionLevel(atr);
            });
    }

    /**
     * Gets the level of ownership a player has over a {@link AbstractDoorBase}.
     *
     * @param player  The player.
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The level of ownership a player has over a {@link AbstractDoorBase}.
     */
    public @NotNull CompletableFuture<Integer> getPermission(final @NotNull IPPlayer player, final long doorUID)
    {
        return getPermission(player.getUUID(), doorUID);
    }

    /**
     * Gets the level of ownership a player has over a {@link AbstractDoorBase}.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @param doorUID    The UID of the {@link AbstractDoorBase}.
     * @return The level of ownership a player has over a {@link AbstractDoorBase}.
     */
    public @NotNull CompletableFuture<Integer> getPermission(final @NotNull UUID playerUUID, final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getPermission(playerUUID.toString(), doorUID), threadPool);
    }

    /**
     * Updates the coordinates of a {@link AbstractDoorBase} in the database.
     *
     * @param doorUID   The UID of the {@link AbstractDoorBase}.
     * @param isOpen    Whether the {@link AbstractDoorBase} is now open or not.
     * @param blockXMin The lower bound x coordinates.
     * @param blockYMin The lower bound y coordinates.
     * @param blockZMin The lower bound z coordinates.
     * @param blockXMax The upper bound x coordinates.
     * @param blockYMax The upper bound y coordinates.
     * @param blockZMax The upper bound z coordinates.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> updateDoorCoords(final long doorUID, final boolean isOpen,
                                                                final int blockXMin,
                                                                final int blockYMin, final int blockZMin,
                                                                final int blockXMax,
                                                                final int blockYMax, final int blockZMax)
    {
        return CompletableFuture.supplyAsync(() -> db.updateDoorCoords(doorUID, isOpen,
                                                                       blockXMin, blockYMin, blockZMin,
                                                                       blockXMax, blockYMax, blockZMax), threadPool);
    }

    /**
     * Adds a player as owner to a {@link AbstractDoorBase} at a given level of ownership.
     *
     * @param door       The {@link AbstractDoorBase}.
     * @param player     The {@link IPPlayer}.
     * @param permission The level of ownership.
     * @return True if owner addition was successful.
     */
    public @NotNull CompletableFuture<Boolean> addOwner(final @NotNull AbstractDoorBase door,
                                                        final @NotNull IPPlayer player,
                                                        final int permission)
    {
        if (permission < 1 || permission > 2)
            return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(
            () ->
            {
                final boolean result = db.addOwner(door.getDoorUID(), player, permission);
                if (result)
                    ((FriendDoorAccessor) door).addOwner(player.getUUID(), new DoorOwner(door.getDoorUID(),
                                                                                         player.getUUID(),
                                                                                         player.getName(), permission));
                return result;
            }, threadPool);
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractDoorBase}.
     *
     * @param door       The {@link AbstractDoorBase}.
     * @param playerUUID The {@link UUID} of the {@link IPPlayer}.
     * @return True if owner removal was successful.
     */
    public @NotNull CompletableFuture<Boolean> removeOwner(final @NotNull AbstractDoorBase door,
                                                           final @NotNull UUID playerUUID)
    {
        final @NotNull Optional<DoorOwner> doorOwner = door.getDoorOwner(playerUUID);
        if (!doorOwner.isPresent())
        {
            PLogger.get().logMessage(Level.FINE,
                                     "Trying to remove player: " + playerUUID + " from door: " + door.getDoorUID() +
                                         ", but the player is not an owner!");
            return CompletableFuture.completedFuture(false);
        }
        if (doorOwner.get().getPermission() == 0)
        {
            PLogger.get().logMessage(Level.FINE,
                                     "Trying to remove player: " + playerUUID + " from door: " + door.getDoorUID() +
                                         ", but the player is the prime owner! This is not allowed!");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(
            () ->
            {
                final boolean result = db.removeOwner(door.getDoorUID(), playerUUID.toString());
                if (result)
                    ((FriendDoorAccessor) door).removeOwner(playerUUID);
                return result;
            }, threadPool);
    }

    /**
     * Updates the opening direction of a {@link AbstractDoorBase}.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @param openDir The new opening direction.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> updateDoorOpenDirection(final long doorUID,
                                                                       final @NotNull RotateDirection openDir)
    {
        return CompletableFuture.supplyAsync(() -> db.updateDoorOpenDirection(doorUID, openDir), threadPool);
    }

    /**
     * Updates the type-specific data of an {@link AbstractDoorBase}. The data will be provided by {@link
     * DoorType#getTypeData(AbstractDoorBase)}.
     *
     * @param door The {@link AbstractDoorBase} whose type-specific data will be updated.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> syncDoorTypeData(final @NotNull AbstractDoorBase door)
    {
        return CompletableFuture.supplyAsync(() -> db.syncTypeData(door), threadPool);
    }

    /**
     * Updates the base data of an {@link AbstractDoorBase}.
     *
     * @param door The {@link AbstractDoorBase} whose base data will be updated.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> syncBaseData(final @NotNull AbstractDoorBase door)
    {
        return CompletableFuture.supplyAsync(() -> db.syncBaseData(door), threadPool);
    }

    /**
     * Updates the all data of an {@link AbstractDoorBase}. This includes both the base data and the type-specific
     * data.
     *
     * @param door The {@link AbstractDoorBase} whose data will be updated.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> syncAllData(final @NotNull AbstractDoorBase door)
    {
        return CompletableFuture.supplyAsync(() -> db.syncAllData(door), threadPool);
    }

    /**
     * Changes the locked status of a {@link AbstractDoorBase}.
     *
     * @param doorUID       The UID of the {@link AbstractDoorBase}.
     * @param newLockStatus The new locked status.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> setLock(final long doorUID, final boolean newLockStatus)
    {
        return CompletableFuture.supplyAsync(() -> db.setLock(doorUID, newLockStatus), threadPool);
    }

    /**
     * Updates the location of a power block of a door. If you want to move the powerblock, it's recommended to use
     * {@link PowerBlockManager#updatePowerBlockLoc} instead, as that will properly invalidate the cache.
     *
     * @param doorUID The UID of the door.
     * @param newLoc  The new location.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    @NotNull CompletableFuture<Boolean> updatePowerBlockLoc(final long doorUID, final @NotNull Vector3DiConst newLoc)
    {
        return CompletableFuture.supplyAsync(() -> db.updateDoorPowerBlockLoc(doorUID, newLoc.getX(), newLoc.getY(),
                                                                              newLoc.getZ()), threadPool);
    }

    /**
     * Checks if a world contains any big doors.
     *
     * @param world The world.
     * @return True if at least 1 door exists in the world.
     */
    @NotNull CompletableFuture<Boolean> isBigDoorsWorld(final @NotNull UUID world)
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
    @NotNull CompletableFuture<ConcurrentHashMap<Integer, List<Long>>> getPowerBlockData(final long chunkHash)
    {
        return CompletableFuture.supplyAsync(() -> db.getPowerBlockData(chunkHash), threadPool);
    }

    /**
     * ONLY used for testing.
     */
    private DatabaseManager(final @NotNull IRestartableHolder restartableHolder)
    {
        super(restartableHolder);
        threadPool = Executors.newSingleThreadExecutor();
        db = null;
        instance = this;
    }

    /**
     * Provides private access to certain aspects of the {@link AbstractDoorBase} class. Kind of like an (inverted, more
     * cumbersome, and less useful) friend in C++ terms.
     */
    // TODO: Consider if this should make work the other way around? That the Door can access the 'private' methods
    //       of this class? This has several advantages:
    //       - The child classes of the door class don't have access to stuff they shouldn't have access to (these methods)
    //       - All the commands that modify a door can be pooled in the AbstractDoorBase class, instead of being split
    //         over several classes.
    //       Alternatively, consider creating a separate class with package-private access to either this class or
    //       the door one. Might be a bit cleaner.
    public static abstract class FriendDoorAccessor
    {
        /**
         * Adds an owner to the map of Owners.
         *
         * @param uuid      The {@link UUID} of the owner.
         * @param doorOwner The {@link DoorOwner} to add.
         */
        protected abstract void addOwner(final @NotNull UUID uuid, final @NotNull DoorOwner doorOwner);

        /**
         * Removes a {@link DoorOwner} from the list of {@link DoorOwner}s, if possible.
         *
         * @param uuid The {@link UUID} of the {@link DoorOwner} that is to be removed.
         * @return True if removal was successful or false if there was no previous {@link DoorOwner} with the provided
         * {@link UUID}.
         */
        protected abstract boolean removeOwner(final @NotNull UUID uuid);
    }
}
