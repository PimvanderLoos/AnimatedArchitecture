package nl.pim16aap2.bigdoors.managers;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    /**
     * The thread pool to use for storage access.
     */
    private final @NonNull ExecutorService threadPool;

    /**
     * The number of threads to use for storage access if the storage allows multithreaded access as determined by
     * {@link IStorage#isSingleThreaded()}.
     */
    private static final int THREADCOUNT = 10;

    private final @NonNull IStorage db;

    /**
     * Constructs a new {@link DatabaseManager}.
     *
     * @param restartableHolder The object managing restarts for this object.
     * @param dbFile            The name of the database file.
     */
    public DatabaseManager(final @NotNull IRestartableHolder restartableHolder, final @NotNull File dbFile)
    {
        this(restartableHolder, new SQLiteJDBCDriverConnection(dbFile));
    }

    /**
     * Constructs a new {@link DatabaseManager}.
     *
     * @param restartableHolder The object managing restarts for this object.
     * @param storage           The {@link IStorage} to use for all database calls.
     */
    public DatabaseManager(final @NonNull IRestartableHolder restartableHolder, final @NotNull IStorage storage)
    {
        super(restartableHolder);
        db = storage;
        if (db.isSingleThreaded())
            threadPool = Executors.newSingleThreadExecutor();
        else
            threadPool = Executors.newFixedThreadPool(THREADCOUNT);
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
                    (door) -> BigDoors.get().getPlatform().getPowerBlockManager()
                                      .onDoorAddOrRemove(door.getWorld().getWorldName(), new Vector3Di(
                                          door.getPowerBlock().getX(),
                                          door.getPowerBlock().getY(),
                                          door.getPowerBlock().getZ())));
                return result;
            }, threadPool).exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Removes a {@link AbstractDoorBase} from the database.
     *
     * @param door The door.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> deleteDoor(final @NotNull AbstractDoorBase door)
    {
        BigDoors.get().getDoorRegistry().deregisterDoor(door.getDoorUID());
        return CompletableFuture.supplyAsync(
            () ->
            {
                boolean result = db.removeDoor(door.getDoorUID());
                if (result)
                    BigDoors.get().getPlatform().getPowerBlockManager()
                            .onDoorAddOrRemove(door.getWorld().getWorldName(),
                                               new Vector3Di(door.getPowerBlock().getX(),
                                                             door.getPowerBlock().getY(),
                                                             door.getPowerBlock().getZ()));
                return result;
            }, threadPool).exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Gets a list of door UIDs that have their engine in a given chunk.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A list of door UIDs that have their engine in a given chunk.
     */
    public @NotNull CompletableFuture<List<Long>> getDoorsInChunk(final long chunkHash)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorsInChunk(chunkHash), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractDoorBase} owned by a player. Only searches for {@link AbstractDoorBase} with a given name
     * if one was provided.
     *
     * @param playerUUID The {@link UUID} of the payer.
     * @param doorID     The name or the UID of the {@link AbstractDoorBase} to search for. Can be null.
     * @return All {@link AbstractDoorBase} owned by a player with a specific name.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID,
                                                                       final @NotNull String doorID)
    {
        // Check if the name is actually the UID of the door.
        final @NotNull OptionalLong doorUID = Util.parseLong(doorID);
        if (doorUID.isPresent())
            return CompletableFuture
                .supplyAsync(() -> db.getDoor(playerUUID, doorUID.getAsLong())
                                     .map(Collections::singletonList)
                                     .orElse(Collections.emptyList()), threadPool)
                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));

        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID, doorID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * See {@link #getDoors(UUID, String)}.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull IPPlayer player,
                                                                       final @NotNull String name)
    {
        return getDoors(player.getUUID(), name);
    }

    /**
     * Gets all {@link AbstractDoorBase} owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @return All {@link AbstractDoorBase} owned by a player.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * See {@link #getDoors(UUID)}.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull IPPlayer player)
    {
        return getDoors(player.getUUID());
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
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID,
                                                                       final @NotNull String name,
                                                                       final int maxPermission)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID, name, maxPermission), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractDoorBase}s with a specific name, regardless over ownership.
     *
     * @param name The name of the {@link AbstractDoorBase}s.
     * @return All {@link AbstractDoorBase}s with a specific name.
     */
    public @NotNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NotNull String name)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(name), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Updates the name of a player in the database, to make sure the player's name and UUID don't go out of sync.
     *
     * @param player The Player.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> updatePlayer(final @NotNull IPPlayer player)
    {
        return CompletableFuture.supplyAsync(() -> db.updatePlayerData(player.getPPlayerData()), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Tries to find the {@link PPlayerData} for a player with the given {@link UUID}.
     *
     * @param uuid The {@link UUID} of a player.
     * @return The {@link PPlayerData} that represents the player.
     */
    public @NonNull CompletableFuture<Optional<PPlayerData>> getPlayerData(final @NonNull UUID uuid)
    {
        return CompletableFuture.supplyAsync(() -> db.getPlayerData(uuid), threadPool)
                                .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Tries to get all the players with a given name. Because names are not unique, this may result in any number of
     * matches.
     * <p>
     * If you know the player's UUID, it is recommended to use {@link #getPlayerData(UUID)} instead.
     *
     * @param playerName The name of the player(s).
     * @return All the players with the given name.
     */
    public @NonNull CompletableFuture<List<PPlayerData>> getPlayerData(final @NonNull String playerName)
    {
        return CompletableFuture.supplyAsync(() -> db.getPlayerData(playerName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets the {@link AbstractDoorBase} with a specific UID.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The {@link AbstractDoorBase} if it exists.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoor(doorUID), threadPool)
                                .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the {@link AbstractDoorBase} with the given UID owned by the player. If the given player does not own the
     * provided door, no door will be returned.
     *
     * @param player  The {@link IPPlayer}.
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The {@link AbstractDoorBase} with the given UID if it exists and the provided player owns it.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @NotNull IPPlayer player,
                                                                          final long doorUID)
    {
        return getDoor(player.getUUID(), doorUID);
    }

    /**
     * Gets the {@link AbstractDoorBase} with the given UID owned by the player. If the given player does not own the *
     * provided door, no door will be returned.
     *
     * @param uuid    The {@link UUID} of the player.
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The {@link AbstractDoorBase} with the given UID if it exists and the provided player owns it.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @NotNull UUID uuid,
                                                                          final long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoor(uuid, doorUID), threadPool)
                                .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the number of {@link AbstractDoorBase}s owned by a player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @return The number of {@link AbstractDoorBase}s this player owns.
     */
    public @NotNull CompletableFuture<Integer> countDoorsOwnedByPlayer(final @NotNull UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
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
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID, doorName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * The number of {@link AbstractDoorBase}s in the database with a specific name.
     *
     * @param doorName The name of the {@link AbstractDoorBase}.
     * @return The number of {@link AbstractDoorBase}s with a specific name.
     */
    public @NotNull CompletableFuture<Integer> countDoorsByName(final @NotNull String doorName)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountByName(doorName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
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
                final @NonNull PPlayerData playerData = player.getPPlayerData();

                final boolean result = db.addOwner(door.getDoorUID(), playerData, permission);
                if (result)
                    ((FriendDoorAccessor) door).addOwner(player.getUUID(), new DoorOwner(door.getDoorUID(),
                                                                                         permission,
                                                                                         playerData));
                return result;
            }, threadPool).exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
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
        if (doorOwner.isEmpty())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                   "Trying to remove player: " + playerUUID + " from door: " +
                                                       door.getDoorUID() +
                                                       ", but the player is not an owner!");
            return CompletableFuture.completedFuture(false);
        }
        if (doorOwner.get().getPermission() == 0)
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                   "Trying to remove player: " + playerUUID + " from door: " +
                                                       door.getDoorUID() +
                                                       ", but the player is the prime owner! This is not allowed!");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(
            () ->
            {
                final boolean result = db.removeOwner(door.getDoorUID(), playerUUID);
                if (result)
                    ((FriendDoorAccessor) door).removeOwner(playerUUID);
                return result;
            }, threadPool).exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Updates the all data of an {@link AbstractDoorBase}. This includes both the base data and the type-specific
     * data.
     *
     * @param simpleDoorData The {@link AbstractDoorBase.SimpleDoorData} that describes the base data of door.
     * @param typeData       The type-specific data of this door.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Boolean> syncDoorData(
        final @NotNull AbstractDoorBase.SimpleDoorData simpleDoorData, final byte[] typeData)
    {
        return CompletableFuture.supplyAsync(() -> db.syncDoorData(simpleDoorData, typeData), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Checks if a world contains any big doors.
     *
     * @param worldName The name of the world.
     * @return True if at least 1 door exists in the world.
     */
    @NotNull CompletableFuture<Boolean> isBigDoorsWorld(final @NotNull String worldName)
    {
        return CompletableFuture.supplyAsync(() -> db.isBigDoorsWorld(worldName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
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
        return CompletableFuture.supplyAsync(() -> db.getPowerBlockData(chunkHash), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, new ConcurrentHashMap<>(0)));
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
