package nl.pim16aap2.bigdoors.managers;

import dagger.Lazy;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOwner;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.events.ICancellableBigDoorsEvent;
import nl.pim16aap2.bigdoors.events.IDoorCreatedEvent;
import nl.pim16aap2.bigdoors.events.IDoorEventCaller;
import nl.pim16aap2.bigdoors.events.IDoorPrepareCreateEvent;
import nl.pim16aap2.bigdoors.events.IDoorPrepareDeleteEvent;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Manages all database interactions.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class DatabaseManager extends Restartable implements IDebuggable
{
    /**
     * The thread pool to use for storage access.
     */
    private volatile ExecutorService threadPool;

    /**
     * The number of threads to use for storage access.
     */
    private static final int THREAD_COUNT = 16;

    private final IStorage db;

    private final IDoorEventCaller doorEventCaller;
    private final DoorRegistry doorRegistry;
    private final Lazy<PowerBlockManager> powerBlockManager;
    private final IBigDoorsEventFactory bigDoorsEventFactory;

    /**
     * Constructs a new {@link DatabaseManager}.
     *
     * @param restartableHolder
     *     The object managing restarts for this object.
     * @param storage
     *     The {@link IStorage} to use for all database calls.
     */
    @Inject
    public DatabaseManager(
        RestartableHolder restartableHolder, IStorage storage, DoorRegistry doorRegistry,
        Lazy<PowerBlockManager> powerBlockManager, IBigDoorsEventFactory bigDoorsEventFactory,
        IDoorEventCaller doorEventCaller, DebuggableRegistry debuggableRegistry)
    {
        super(restartableHolder);
        db = storage;
        this.doorEventCaller = doorEventCaller;
        this.doorRegistry = doorRegistry;
        this.powerBlockManager = powerBlockManager;
        this.bigDoorsEventFactory = bigDoorsEventFactory;
        initThreadPool();
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Obtains {@link IStorage.DatabaseState} the database is in.
     *
     * @return The {@link IStorage.DatabaseState} the database is in.
     */
    public IStorage.DatabaseState getDatabaseState()
    {
        return db.getDatabaseState();
    }

    @Override
    public void initialize()
    {
        initThreadPool();
    }

    @Override
    public void shutDown()
    {
        threadPool.shutdownNow();
    }

    private void initThreadPool()
    {
        this.threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    /**
     * Inserts a {@link AbstractDoor} into the database and assumes that the door was NOT created by an
     * {@link IPPlayer}. See {@link #addDoor(AbstractDoor, IPPlayer)}.
     *
     * @param newDoor
     *     The new {@link AbstractDoor}.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public CompletableFuture<DoorInsertResult> addDoor(AbstractDoor newDoor)
    {
        return addDoor(newDoor, null);
    }

    /**
     * Inserts a {@link AbstractDoor} into the database.
     *
     * @param newDoor
     *     The new {@link AbstractDoor}.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the door. This is used for the {@link IDoorPrepareCreateEvent}
     *     and the {@link IDoorCreatedEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<DoorInsertResult> addDoor(AbstractDoor newDoor, @Nullable IPPlayer responsible)
    {
        final var ret = callCancellableEvent(
            fact -> fact.createPrepareDoorCreateEvent(newDoor, responsible)).thenApplyAsync(
            cancelled ->
            {
                if (cancelled)
                    return new DoorInsertResult(Optional.empty(), true);

                final Optional<AbstractDoor> result = db.insert(newDoor);
                result.ifPresent(
                    (door) -> powerBlockManager.get().onDoorAddOrRemove(door.getWorld().worldName(),
                                                                        new Vector3Di(door.getPowerBlock().x(),
                                                                                      door.getPowerBlock().y(),
                                                                                      door.getPowerBlock().z())));
                return new DoorInsertResult(result, false);
            }, threadPool).exceptionally(ex -> Util.exceptionally(ex, new DoorInsertResult(Optional.empty(), false)));

        ret.thenAccept(result -> callDoorCreatedEvent(result, responsible));

        return ret;
    }

    /**
     * Calls the {@link IDoorCreatedEvent}.
     *
     * @param result
     *     The result of trying to add a door to the database.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating it, if an {@link IPPlayer} was responsible for it. If not, this
     *     is null.
     */
    private void callDoorCreatedEvent(DoorInsertResult result, @Nullable IPPlayer responsible)
    {
        CompletableFuture.runAsync(
            () ->
            {
                if (result.cancelled() || result.door().isEmpty())
                    return;

                final IDoorCreatedEvent doorCreatedEvent =
                    bigDoorsEventFactory.createDoorCreatedEvent(result.door().get(), responsible);

                doorEventCaller.callDoorEvent(doorCreatedEvent);
            });
    }

    /**
     * Removes a {@link AbstractDoor} from the database and assumes that the door was NOT deleted by an
     * {@link IPPlayer}. See {@link #deleteDoor(AbstractDoor, IPPlayer)}.
     *
     * @param door
     *     The door.
     * @return The future result of the operation.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<ActionResult> deleteDoor(AbstractDoor door)
    {
        return deleteDoor(door, null);
    }

    /**
     * Removes a {@link AbstractDoor} from the database.
     *
     * @param door
     *     The door that will be deleted.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the door. This is used for the {@link IDoorPrepareDeleteEvent}.
     *     This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> deleteDoor(AbstractDoor door, @Nullable IPPlayer responsible)
    {
        return callCancellableEvent(fact -> fact.createPrepareDeleteDoorEvent(door, responsible)).thenApplyAsync(
            cancelled ->
            {
                if (cancelled)
                    return ActionResult.CANCELLED;

                doorRegistry.deregisterDoor(door.getDoorUID());
                final boolean result = db.removeDoor(door.getDoorUID());
                if (!result)
                    return ActionResult.FAIL;

                powerBlockManager.get().onDoorAddOrRemove(door.getWorld().worldName(),
                                                          new Vector3Di(door.getPowerBlock().x(),
                                                                        door.getPowerBlock().y(),
                                                                        door.getPowerBlock().z()));
                return ActionResult.SUCCESS;
            }, threadPool).exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Gets a list of door UIDs that have their rotation point in a given chunk.
     *
     * @param chunkId
     *     The id of the chunk the doors are in.
     * @return A list of door UIDs that have their rotation point in a given chunk.
     */
    public CompletableFuture<List<Long>> getDoorsInChunk(long chunkId)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorsInChunk(chunkId), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractDoor} owned by a player. Only searches for {@link AbstractDoor} with a given name if one
     * was provided.
     *
     * @param playerUUID
     *     The {@link UUID} of the payer.
     * @param doorID
     *     The name or the UID of the {@link AbstractDoor} to search for. Can be null.
     * @return All {@link AbstractDoor} owned by a player with a specific name.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors(UUID playerUUID, String doorID)
    {
        // Check if the name is actually the UID of the door.
        final OptionalLong doorUID = Util.parseLong(doorID);
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
    public CompletableFuture<List<AbstractDoor>> getDoors(IPPlayer player, String name)
    {
        return getDoors(player.getUUID(), name);
    }

    /**
     * Gets all {@link AbstractDoor} owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return All {@link AbstractDoor} owned by a player.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors(UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * See {@link #getDoors(UUID)}.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors(IPPlayer player)
    {
        return getDoors(player.getUUID());
    }

    /**
     * Gets all {@link AbstractDoor} owned by a player with a specific name.
     *
     * @param playerUUID
     *     The {@link UUID} of the payer.
     * @param name
     *     The name of the {@link AbstractDoor} to search for.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the {@link AbstractDoor}s.
     * @return All {@link AbstractDoor} owned by a player with a specific name.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors(UUID playerUUID, String name, PermissionLevel maxPermission)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(playerUUID, name, maxPermission), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractDoor}s with a specific name, regardless over ownership.
     *
     * @param name
     *     The name of the {@link AbstractDoor}s.
     * @return All {@link AbstractDoor}s with a specific name.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors(String name)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoors(name), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Updates the name of a player in the database, to make sure the player's name and UUID don't go out of sync.
     *
     * @param player
     *     The Player.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public CompletableFuture<Boolean> updatePlayer(IPPlayer player)
    {
        return CompletableFuture.supplyAsync(() -> db.updatePlayerData(player.getPPlayerData()), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Tries to find the {@link PPlayerData} for a player with the given {@link UUID}.
     *
     * @param uuid
     *     The {@link UUID} of a player.
     * @return The {@link PPlayerData} that represents the player.
     */
    public CompletableFuture<Optional<PPlayerData>> getPlayerData(UUID uuid)
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
     * @param playerName
     *     The name of the player(s).
     * @return All the players with the given name.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<List<PPlayerData>> getPlayerData(String playerName)
    {
        return CompletableFuture.supplyAsync(() -> db.getPlayerData(playerName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets the {@link AbstractDoor} with a specific UID.
     *
     * @param doorUID
     *     The UID of the {@link AbstractDoor}.
     * @return The {@link AbstractDoor} if it exists.
     */
    public CompletableFuture<Optional<AbstractDoor>> getDoor(long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoor(doorUID), threadPool)
                                .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the {@link AbstractDoor} with the given UID owned by the player. If the given player does not own the
     * provided door, no door will be returned.
     *
     * @param player
     *     The {@link IPPlayer}.
     * @param doorUID
     *     The UID of the {@link AbstractDoor}.
     * @return The {@link AbstractDoor} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player, long doorUID)
    {
        return getDoor(player.getUUID(), doorUID);
    }

    /**
     * Gets the {@link AbstractDoor} with the given UID owned by the player. If the given player does not own the *
     * provided door, no door will be returned.
     *
     * @param uuid
     *     The {@link UUID} of the player.
     * @param doorUID
     *     The UID of the {@link AbstractDoor}.
     * @return The {@link AbstractDoor} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<AbstractDoor>> getDoor(UUID uuid, long doorUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoor(uuid, doorUID), threadPool)
                                .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the number of {@link AbstractDoor}s owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return The number of {@link AbstractDoor}s this player owns.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countDoorsOwnedByPlayer(UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * Counts the number of {@link AbstractDoor}s with a specific name owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @param doorName
     *     The name of the door.
     * @return The number of {@link AbstractDoor}s with a specific name owned by a player.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countDoorsOwnedByPlayer(UUID playerUUID, String doorName)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountForPlayer(playerUUID, doorName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * The number of {@link AbstractDoor}s in the database with a specific name.
     *
     * @param doorName
     *     The name of the {@link AbstractDoor}.
     * @return The number of {@link AbstractDoor}s with a specific name.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countDoorsByName(String doorName)
    {
        return CompletableFuture.supplyAsync(() -> db.getDoorCountByName(doorName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * Adds a player as owner to a {@link AbstractDoor} at a given level of ownership and assumes that the door was NOT
     * deleted by an {@link IPPlayer}. See {@link #addOwner(AbstractDoor, IPPlayer, PermissionLevel, IPPlayer)}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param player
     *     The {@link IPPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(AbstractDoor door, IPPlayer player, PermissionLevel permission)
    {
        return addOwner(door, player, permission, null);
    }

    /**
     * Adds a player as owner to a {@link AbstractDoor} at a given level of ownership.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param player
     *     The {@link IPPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(
        AbstractDoor door, IPPlayer player, PermissionLevel permission, @Nullable IPPlayer responsible)
    {
        if (permission.getValue() < 1 || permission == PermissionLevel.NO_PERMISSION)
            return CompletableFuture.completedFuture(ActionResult.FAIL);

        final var newOwner = new DoorOwner(door.getDoorUID(), permission, player.getPPlayerData());

        return callCancellableEvent(fact -> fact.createDoorPrepareAddOwnerEvent(door, newOwner, responsible))
            .thenApplyAsync(
                cancelled ->
                {
                    if (cancelled)
                        return ActionResult.CANCELLED;

                    final PPlayerData playerData = player.getPPlayerData();

                    final boolean result = db.addOwner(door.getDoorUID(), playerData, permission);
                    if (!result)
                        return ActionResult.FAIL;

                    ((FriendDoorAccessor) door.getDoorBase())
                        .addOwner(player.getUUID(), new DoorOwner(door.getDoorUID(), permission, playerData));

                    return ActionResult.SUCCESS;
                }, threadPool).exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Calls an {@link ICancellableBigDoorsEvent} and checks if it was cancelled or not.
     *
     * @param factoryMethod
     *     The method to use to construct the event.
     * @return True if the create event was cancelled, otherwise false.
     */
    private CompletableFuture<Boolean> callCancellableEvent(
        Function<IBigDoorsEventFactory, ICancellableBigDoorsEvent> factoryMethod)
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                final var event = factoryMethod.apply(bigDoorsEventFactory);
                doorEventCaller.callDoorEvent(event);
                log.at(Level.SEVERE).log("Event %s was%s cancelled!", event, (event.isCancelled() ? "" : " not"));
                return event.isCancelled();
            });
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractDoor}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param player
     *     The {@link IPPlayer}.
     * @return True if owner removal was successful.
     */
    public CompletableFuture<ActionResult> removeOwner(AbstractDoor door, IPPlayer player)
    {
        return removeOwner(door, player, null);
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractDoor}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param player
     *     The {@link IPPlayer}.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the door. This is used for the {@link IDoorPrepareDeleteEvent}.
     *     This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        AbstractDoor door, IPPlayer player, @Nullable IPPlayer responsible)
    {
        return removeOwner(door, player.getUUID(), responsible);
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractDoor} and assumes that the door was NOT deleted by an
     * {@link IPPlayer}. See {@link #removeOwner(AbstractDoor, UUID, IPPlayer)}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPPlayer}.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(AbstractDoor door, UUID playerUUID)
    {
        return removeOwner(door, playerUUID, null);
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractDoor}.
     *
     * @param door
     *     The {@link AbstractDoor}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPPlayer}.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the door. This is used for the {@link IDoorPrepareDeleteEvent}.
     *     This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        AbstractDoor door, UUID playerUUID, @Nullable IPPlayer responsible)
    {
        final Optional<DoorOwner> doorOwner = door.getDoorOwner(playerUUID);
        if (doorOwner.isEmpty())
        {
            log.at(Level.FINE).log("Trying to remove player: %s from door: %d, but the player is not an owner!",
                                   playerUUID, door.getDoorUID());
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }
        if (doorOwner.get().permission() == PermissionLevel.CREATOR)
        {
            log.at(Level.FINE).log("Trying to remove player: %s from door: %d, but the player is the prime owner! " +
                                       "This is not allowed!",
                                   playerUUID, door.getDoorUID());
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }

        return callCancellableEvent(fact -> fact.createDoorPrepareRemoveOwnerEvent(door, doorOwner.get(), responsible))
            .thenApplyAsync(
                cancelled ->
                {
                    if (cancelled)
                        return ActionResult.CANCELLED;

                    final boolean result = db.removeOwner(door.getDoorUID(), playerUUID);
                    if (!result)
                        return ActionResult.FAIL;

                    ((FriendDoorAccessor) door.getDoorBase()).removeOwner(playerUUID);
                    return ActionResult.SUCCESS;
                }, threadPool).exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Updates the all data of an {@link AbstractDoor}. This includes both the base data and the type-specific data.
     *
     * @param doorBase
     *     The {@link DoorBase} that describes the base data of door.
     * @param typeData
     *     The type-specific data of this door.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public CompletableFuture<Boolean> syncDoorData(DoorBase doorBase, byte[] typeData)
    {
        return CompletableFuture.supplyAsync(() -> db.syncDoorData(doorBase, typeData), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Retrieves all {@link DoorIdentifier}s that start with the provided input.
     * <p>
     * For example, this method can retrieve the identifiers "1", "10", "11", "100", etc from an input of "1" or
     * "MyDoor", "MyPortcullis", "MyOtherDoor", etc. from an input of "My".
     *
     * @param input
     *     The partial identifier to look for.
     * @param player
     *     The player that should own the doors. May be null to disregard ownership.
     * @return All {@link DoorIdentifier}s that start with the provided input.
     */
    public CompletableFuture<List<DoorIdentifier>> getIdentifiersFromPartial(
        String input, @Nullable IPPlayer player, PermissionLevel maxPermission)
    {
        return CompletableFuture.supplyAsync(() -> db.getPartialIdentifiers(input, player, maxPermission), threadPool)
                                .exceptionally(t -> Util.exceptionally(t, Collections.emptyList()));
    }

    /**
     * Checks if a world contains any big doors.
     *
     * @param worldName
     *     The name of the world.
     * @return True if at least 1 door exists in the world.
     */
    CompletableFuture<Boolean> isBigDoorsWorld(String worldName)
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
     * @param chunkId
     *     The id of the chunk the doors are in.
     * @return A map of location hashes and their connected powerblocks for all doors in a chunk.
     */
    CompletableFuture<ConcurrentHashMap<Integer, List<Long>>> getPowerBlockData(long chunkId)
    {
        return CompletableFuture.supplyAsync(() -> db.getPowerBlockData(chunkId), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, new ConcurrentHashMap<>(0)));
    }

    @Override
    public String getDebugInformation()
    {
        return "Database status: " + threadPool;
    }

    /**
     * Represents the result of an action requested from the database. E.g. deleting a door.
     */
    public enum ActionResult
    {
        /**
         * The request was cancelled. E.g. by an {@link ICancellableBigDoorsEvent} event.
         */
        CANCELLED,

        /**
         * Success! Everything went as expected.
         */
        SUCCESS,

        /**
         * Something went wrong. Check the logs?
         */
        FAIL
    }

    /**
     * Provides private access to certain aspects of the {@link AbstractDoor} class. Kind of like an (inverted, more
     * cumbersome, and less useful) friend in C++ terms.
     */
    // TODO: Consider if this should make work the other way around? That the Door can access the 'private' methods
    //       of this class? This has several advantages:
    //       - The child classes of the door class don't have access to stuff they shouldn't have access to (these
    //         methods)
    //       - All the commands that modify a door can be pooled in the AbstractDoor class, instead of being split
    //         over several classes.
    //       Alternatively, consider creating a separate class with package-private access to either this class or
    //       the door one. Might be a bit cleaner.
    public abstract static class FriendDoorAccessor
    {
        /**
         * Adds an owner to the map of Owners.
         *
         * @param uuid
         *     The {@link UUID} of the owner.
         * @param doorOwner
         *     The {@link DoorOwner} to add.
         */
        protected abstract void addOwner(UUID uuid, DoorOwner doorOwner);

        /**
         * Removes a {@link DoorOwner} from the list of {@link DoorOwner}s, if possible.
         *
         * @param uuid
         *     The {@link UUID} of the {@link DoorOwner} that is to be removed.
         * @return True if removal was successful or false if there was no previous {@link DoorOwner} with the provided
         * {@link UUID}.
         */
        protected abstract boolean removeOwner(UUID uuid);
    }

    /**
     * Contains the result of an attempt to insert a door into the database.
     */
    @AllArgsConstructor @EqualsAndHashCode @ToString
    public static final class DoorInsertResult
    {
        /**
         * The door as it was inserted into the database. This will be empty when inserted failed.
         */
        private final Optional<AbstractDoor> door;
        /**
         * Whether the insertion was cancelled. An insertion may be cancelled if some listener cancels the
         * {@link IDoorPrepareCreateEvent} event.
         */
        private final boolean cancelled;

        /**
         * See {@link #door}.
         */
        public Optional<AbstractDoor> door()
        {
            return door;
        }

        /**
         * See {@link #cancelled}.
         */
        public boolean cancelled()
        {
            return cancelled;
        }
    }

    @AllArgsConstructor @EqualsAndHashCode @ToString
    public static final class DoorIdentifier
    {
        private final long uid;
        private final String name;

        public long uid()
        {
            return uid;
        }

        public String name()
        {
            return name;
        }
    }
}
