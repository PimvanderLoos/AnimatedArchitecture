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
import nl.pim16aap2.bigdoors.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.events.ICancellableBigDoorsEvent;
import nl.pim16aap2.bigdoors.events.IMovableCreatedEvent;
import nl.pim16aap2.bigdoors.events.IMovablePrepareCreateEvent;
import nl.pim16aap2.bigdoors.events.IMovablePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
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

    private final IBigDoorsEventCaller bigDoorsEventCaller;
    private final MovableRegistry movableRegistry;
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
        RestartableHolder restartableHolder, IStorage storage, MovableRegistry movableRegistry,
        Lazy<PowerBlockManager> powerBlockManager, IBigDoorsEventFactory bigDoorsEventFactory,
        IBigDoorsEventCaller bigDoorsEventCaller, DebuggableRegistry debuggableRegistry)
    {
        super(restartableHolder);
        db = storage;
        this.bigDoorsEventCaller = bigDoorsEventCaller;
        this.movableRegistry = movableRegistry;
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
     * Inserts a {@link AbstractMovable} into the database and assumes that the movable was NOT created by an
     * {@link IPPlayer}. See {@link #addMovable(AbstractMovable, IPPlayer)}.
     *
     * @param movable
     *     The new {@link AbstractMovable}.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public CompletableFuture<MovableInsertResult> addMovable(AbstractMovable movable)
    {
        return addMovable(movable, null);
    }

    /**
     * Inserts a {@link AbstractMovable} into the database.
     *
     * @param movable
     *     The new {@link AbstractMovable}.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the movable. This is used for the
     *     {@link IMovablePrepareCreateEvent} and the {@link IMovableCreatedEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<MovableInsertResult> addMovable(AbstractMovable movable, @Nullable IPPlayer responsible)
    {
        final var ret = callCancellableEvent(
            fact -> fact.createPrepareMovableCreateEvent(movable, responsible)).thenApplyAsync(
            cancelled ->
            {
                if (cancelled)
                    return new MovableInsertResult(Optional.empty(), true);

                final Optional<AbstractMovable> result = db.insert(movable);
                result.ifPresent(
                    (newMovable) -> powerBlockManager.get().onMovableAddOrRemove(
                        newMovable.getWorld().worldName(),
                        new Vector3Di(newMovable.getPowerBlock().x(),
                                      newMovable.getPowerBlock().y(),
                                      newMovable.getPowerBlock().z())));
                return new MovableInsertResult(result, false);
            }, threadPool).exceptionally(
            ex -> Util.exceptionally(ex, new MovableInsertResult(Optional.empty(), false)));

        ret.thenAccept(result -> callMovableCreatedEvent(result, responsible));

        return ret;
    }

    /**
     * Calls the {@link IMovableCreatedEvent}.
     *
     * @param result
     *     The result of trying to add a movable to the database.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating it, if an {@link IPPlayer} was responsible for it. If not, this
     *     is null.
     */
    private void callMovableCreatedEvent(MovableInsertResult result, @Nullable IPPlayer responsible)
    {
        CompletableFuture.runAsync(
            () ->
            {
                if (result.cancelled() || result.movable().isEmpty())
                    return;

                final IMovableCreatedEvent movableCreatedEvent =
                    bigDoorsEventFactory.createMovableCreatedEvent(result.movable().get(), responsible);

                bigDoorsEventCaller.callBigDoorsEvent(movableCreatedEvent);
            });
    }

    /**
     * Removes a {@link AbstractMovable} from the database and assumes that the movable was NOT deleted by an
     * {@link IPPlayer}. See {@link #deleteMovable(AbstractMovable, IPPlayer)}.
     *
     * @param movable
     *     The movable that will be deleted.
     * @return The future result of the operation.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<ActionResult> deleteMovable(AbstractMovable movable)
    {
        return deleteMovable(movable, null);
    }

    /**
     * Removes a {@link AbstractMovable} from the database.
     *
     * @param movable
     *     The movable that will be deleted.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the movable. This is used for the
     *     {@link IMovablePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> deleteMovable(AbstractMovable movable, @Nullable IPPlayer responsible)
    {
        return callCancellableEvent(fact -> fact.createPrepareDeleteMovableEvent(movable, responsible)).thenApplyAsync(
            cancelled ->
            {
                if (cancelled)
                    return ActionResult.CANCELLED;

                movableRegistry.deregisterMovable(movable.getUid());
                final boolean result = db.removeMovable(movable.getUid());
                if (!result)
                    return ActionResult.FAIL;

                powerBlockManager.get().onMovableAddOrRemove(movable.getWorld().worldName(),
                                                             new Vector3Di(movable.getPowerBlock().x(),
                                                                           movable.getPowerBlock().y(),
                                                                           movable.getPowerBlock().z()));
                return ActionResult.SUCCESS;
            }, threadPool).exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Gets a list of movable UIDs that have their rotation point in a given chunk.
     *
     * @param chunkId
     *     The id of the chunk the movables are in.
     * @return A list of movable UIDs that have their rotation point in a given chunk.
     */
    public CompletableFuture<List<Long>> getMovablesInChunk(long chunkId)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovablesInChunk(chunkId), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractMovable} owned by a player. Only searches for {@link AbstractMovable} with a given name
     * if one was provided.
     *
     * @param playerUUID
     *     The {@link UUID} of the payer.
     * @param movableID
     *     The name or the UID of the {@link AbstractMovable} to search for. Can be null.
     * @return All {@link AbstractMovable} owned by a player with a specific name.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(UUID playerUUID, String movableID)
    {
        // Check if the name is actually the UID of the movable.
        final OptionalLong movableUID = Util.parseLong(movableID);
        if (movableUID.isPresent())
            return CompletableFuture
                .supplyAsync(() -> db.getMovable(playerUUID, movableUID.getAsLong())
                                     .map(Collections::singletonList)
                                     .orElse(Collections.emptyList()), threadPool)
                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));

        return CompletableFuture.supplyAsync(() -> db.getMovables(playerUUID, movableID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * See {@link #getMovables(UUID, String)}.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(IPPlayer player, String name)
    {
        return getMovables(player.getUUID(), name);
    }

    /**
     * Gets all {@link AbstractMovable} owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return All {@link AbstractMovable} owned by a player.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovables(playerUUID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * See {@link #getMovables(UUID)}.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(IPPlayer player)
    {
        return getMovables(player.getUUID());
    }

    /**
     * Gets all {@link AbstractMovable} owned by a player with a specific name.
     *
     * @param playerUUID
     *     The {@link UUID} of the payer.
     * @param name
     *     The name of the {@link AbstractMovable} to search for.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the {@link AbstractMovable}s.
     * @return All {@link AbstractMovable} owned by a player with a specific name.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(
        UUID playerUUID, String name, PermissionLevel maxPermission)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovables(playerUUID, name, maxPermission), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractMovable}s with a specific name, regardless over ownership.
     *
     * @param name
     *     The name of the {@link AbstractMovable}s.
     * @return All {@link AbstractMovable}s with a specific name.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(String name)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovables(name), threadPool)
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
     * Gets the {@link AbstractMovable} with a specific UID.
     *
     * @param movableUID
     *     The UID of the {@link AbstractMovable}.
     * @return The {@link AbstractMovable} if it exists.
     */
    public CompletableFuture<Optional<AbstractMovable>> getMovable(long movableUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovable(movableUID), threadPool)
                                .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the {@link AbstractMovable} with the given UID owned by the player. If the given player does not own the
     * provided movable, no movable will be returned.
     *
     * @param player
     *     The {@link IPPlayer}.
     * @param movableUID
     *     The UID of the {@link AbstractMovable}.
     * @return The {@link AbstractMovable} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player, long movableUID)
    {
        return getMovable(player.getUUID(), movableUID);
    }

    /**
     * Gets the {@link AbstractMovable} with the given UID owned by the player. If the given player does not own the *
     * provided movable, no movable will be returned.
     *
     * @param uuid
     *     The {@link UUID} of the player.
     * @param movableUID
     *     The UID of the {@link AbstractMovable}.
     * @return The {@link AbstractMovable} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<AbstractMovable>> getMovable(UUID uuid, long movableUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovable(uuid, movableUID), threadPool)
                                .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the number of {@link AbstractMovable}s owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return The number of {@link AbstractMovable}s this player owns.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countMovablesOwnedByPlayer(UUID playerUUID)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovableCountForPlayer(playerUUID), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * Counts the number of {@link AbstractMovable}s with a specific name owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @param movableName
     *     The name of the movable.
     * @return The number of {@link AbstractMovable}s with a specific name owned by a player.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countMovablesOwnedByPlayer(UUID playerUUID, String movableName)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovableCountForPlayer(playerUUID, movableName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * The number of {@link AbstractMovable}s in the database with a specific name.
     *
     * @param movableName
     *     The name of the {@link AbstractMovable}.
     * @return The number of {@link AbstractMovable}s with a specific name.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countMovablesByName(String movableName)
    {
        return CompletableFuture.supplyAsync(() -> db.getMovableCountByName(movableName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * Adds a player as owner to a {@link AbstractMovable} at a given level of ownership and assumes that the movable
     * was NOT deleted by an {@link IPPlayer}. See
     * {@link #addOwner(AbstractMovable, IPPlayer, PermissionLevel, IPPlayer)}.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param player
     *     The {@link IPPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(
        AbstractMovable movable, IPPlayer player, PermissionLevel permission)
    {
        return addOwner(movable, player, permission, null);
    }

    /**
     * Adds a player as owner to a {@link AbstractMovable} at a given level of ownership.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param player
     *     The {@link IPPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(
        AbstractMovable movable, IPPlayer player, PermissionLevel permission, @Nullable IPPlayer responsible)
    {
        if (permission.getValue() < 1 || permission == PermissionLevel.NO_PERMISSION)
            return CompletableFuture.completedFuture(ActionResult.FAIL);

        final var newOwner = new MovableOwner(movable.getUid(), permission, player.getPPlayerData());

        return callCancellableEvent(fact -> fact.createMovablePrepareAddOwnerEvent(movable, newOwner, responsible))
            .thenApplyAsync(
                cancelled ->
                {
                    if (cancelled)
                        return ActionResult.CANCELLED;

                    final PPlayerData playerData = player.getPPlayerData();

                    final boolean result = db.addOwner(movable.getUid(), playerData, permission);
                    if (!result)
                        return ActionResult.FAIL;

                    ((FriendMovableAccessor) movable.getMovableBase())
                        .addOwner(player.getUUID(), new MovableOwner(movable.getUid(), permission, playerData));

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
                bigDoorsEventCaller.callBigDoorsEvent(event);
                log.at(Level.SEVERE).log("Event %s was%s cancelled!", event, (event.isCancelled() ? "" : " not"));
                return event.isCancelled();
            });
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractMovable}.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param player
     *     The {@link IPPlayer}.
     * @return True if owner removal was successful.
     */
    public CompletableFuture<ActionResult> removeOwner(AbstractMovable movable, IPPlayer player)
    {
        return removeOwner(movable, player, null);
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractMovable}.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param player
     *     The {@link IPPlayer}.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the movable. This is used for the
     *     {@link IMovablePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        AbstractMovable movable, IPPlayer player, @Nullable IPPlayer responsible)
    {
        return removeOwner(movable, player.getUUID(), responsible);
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractMovable} and assumes that the movable was NOT deleted by
     * an {@link IPPlayer}. See {@link #removeOwner(AbstractMovable, UUID, IPPlayer)}.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPPlayer}.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(AbstractMovable movable, UUID playerUUID)
    {
        return removeOwner(movable, playerUUID, null);
    }

    /**
     * Remove a {@link IPPlayer} as owner of a {@link AbstractMovable}.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPPlayer}.
     * @param responsible
     *     The {@link IPPlayer} responsible for creating the movable. This is used for the
     *     {@link IMovablePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        AbstractMovable movable, UUID playerUUID, @Nullable IPPlayer responsible)
    {
        final Optional<MovableOwner> movableOwner = movable.getOwner(playerUUID);
        if (movableOwner.isEmpty())
        {
            log.at(Level.FINE).log("Trying to remove player: %s from movable: %d, but the player is not an owner!",
                                   playerUUID, movable.getUid());
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }
        if (movableOwner.get().permission() == PermissionLevel.CREATOR)
        {
            log.at(Level.FINE).log("Trying to remove player: %s from movable: %d, but the player is the prime owner! " +
                                       "This is not allowed!",
                                   playerUUID, movable.getUid());
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }

        return callCancellableEvent(
            fact -> fact.createMovablePrepareRemoveOwnerEvent(movable, movableOwner.get(), responsible))
            .thenApplyAsync(
                cancelled ->
                {
                    if (cancelled)
                        return ActionResult.CANCELLED;

                    final boolean result = db.removeOwner(movable.getUid(), playerUUID);
                    if (!result)
                        return ActionResult.FAIL;

                    ((FriendMovableAccessor) movable.getMovableBase()).removeOwner(playerUUID);
                    return ActionResult.SUCCESS;
                }, threadPool).exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Updates the all data of an {@link AbstractMovable}. This includes both the base data and the type-specific data.
     *
     * @param snapshot
     *     The {@link MovableBase} that describes the base data of movable.
     * @param typeData
     *     The type-specific data of this movable.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public CompletableFuture<Boolean> syncMovableData(MovableSnapshot snapshot, byte[] typeData)
    {
        return CompletableFuture.supplyAsync(() -> db.syncMovableData(snapshot, typeData), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Retrieves all {@link MovableIdentifier}s that start with the provided input.
     * <p>
     * For example, this method can retrieve the identifiers "1", "10", "11", "100", etc from an input of "1" or
     * "MyDoor", "MyPortcullis", "MyOtherMovable", etc. from an input of "My".
     *
     * @param input
     *     The partial identifier to look for.
     * @param player
     *     The player that should own the movables. May be null to disregard ownership.
     * @return All {@link MovableIdentifier}s that start with the provided input.
     */
    public CompletableFuture<List<MovableIdentifier>> getIdentifiersFromPartial(
        String input, @Nullable IPPlayer player, PermissionLevel maxPermission)
    {
        return CompletableFuture.supplyAsync(() -> db.getPartialIdentifiers(input, player, maxPermission), threadPool)
                                .exceptionally(t -> Util.exceptionally(t, Collections.emptyList()));
    }

    /**
     * Checks if a world contains any movables.
     *
     * @param worldName
     *     The name of the world.
     * @return True if at least 1 movable exists in the world.
     */
    CompletableFuture<Boolean> isBigDoorsWorld(String worldName)
    {
        return CompletableFuture.supplyAsync(() -> db.isBigDoorsWorld(worldName), threadPool)
                                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Gets a map of location hashes and their connected powerblocks for all movables in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the movables whose powerblocks
     * occupies that location.
     *
     * @param chunkId
     *     The id of the chunk the movables are in.
     * @return A map of location hashes and their connected powerblocks for all movables in a chunk.
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
     * Represents the result of an action requested from the database. E.g. deleting a movable.
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
     * Provides private access to certain aspects of the {@link AbstractMovable} class. Kind of like an (inverted, more
     * cumbersome, and less useful) friend in C++ terms.
     */
    // TODO: Consider if this should make work the other way around? That the Movable can access the 'private' methods
    //       of this class? This has several advantages:
    //       - The child classes of the movable class don't have access to stuff they shouldn't have access to (these
    //         methods)
    //       - All the commands that modify a movable can be pooled in the AbstractMovable class, instead of being split
    //         over several classes.
    //       Alternatively, consider creating a separate class with package-private access to either this class or
    //       the movable one. Might be a bit cleaner.
    public abstract static class FriendMovableAccessor
    {
        /**
         * Adds an owner to the map of Owners.
         *
         * @param uuid
         *     The {@link UUID} of the owner.
         * @param movableOwner
         *     The {@link MovableOwner} to add.
         */
        protected abstract void addOwner(UUID uuid, MovableOwner movableOwner);

        /**
         * Removes a {@link MovableOwner} from the list of {@link MovableOwner}s, if possible.
         *
         * @param uuid
         *     The {@link UUID} of the {@link MovableOwner} that is to be removed.
         * @return True if removal was successful or false if there was no previous {@link MovableOwner} with the
         * provided {@link UUID}.
         */
        protected abstract boolean removeOwner(UUID uuid);
    }

    /**
     * Contains the result of an attempt to insert a movable into the database.
     */
    @AllArgsConstructor @EqualsAndHashCode @ToString
    public static final class MovableInsertResult
    {
        /**
         * The movable as it was inserted into the database. This will be empty when inserted failed.
         */
        private final Optional<AbstractMovable> movable;
        /**
         * Whether the insertion was cancelled. An insertion may be cancelled if some listener cancels the
         * {@link IMovablePrepareCreateEvent} event.
         */
        private final boolean cancelled;

        /**
         * See {@link #movable}.
         */
        public Optional<AbstractMovable> movable()
        {
            return movable;
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
    public static final class MovableIdentifier
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
