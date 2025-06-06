package nl.pim16aap2.animatedarchitecture.core.managers;

import com.google.common.flogger.StackSize;
import dagger.Lazy;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.events.ICancellableAnimatedArchitectureEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureCreatedEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareCreateEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareDeleteEvent;
import nl.pim16aap2.animatedarchitecture.core.storage.IStorage;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureModifier;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.LocationUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Manages all database interactions.
 */
@Singleton
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public final class DatabaseManager extends Restartable implements IDebuggable
{
    /**
     * The thread pool to use for storage access.
     * <p>
     * When scheduling tasks that are not related to the database, use {@link IExecutor#getVirtualExecutor()} instead
     * (or create a new executor if necessary).
     */
    private volatile ExecutorService threadPool;

    private final IExecutor executor;
    private final IStorage db;
    private final StructureDeletionManager structureDeletionManager;
    private final IAnimatedArchitectureEventCaller animatedArchitectureEventCaller;
    private final Lazy<PowerBlockManager> powerBlockManager;
    private final IAnimatedArchitectureEventFactory animatedArchitectureEventFactory;
    private final StructureModifier structureModifier;

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
        IExecutor executor,
        RestartableHolder restartableHolder,
        IStorage storage,
        StructureDeletionManager structureDeletionManager,
        Lazy<PowerBlockManager> powerBlockManager,
        IAnimatedArchitectureEventFactory animatedArchitectureEventFactory,
        IAnimatedArchitectureEventCaller animatedArchitectureEventCaller,
        DebuggableRegistry debuggableRegistry)
    {
        super(restartableHolder);
        this.executor = executor;
        this.db = storage;
        this.structureDeletionManager = structureDeletionManager;
        this.animatedArchitectureEventCaller = animatedArchitectureEventCaller;
        this.powerBlockManager = powerBlockManager;
        this.animatedArchitectureEventFactory = animatedArchitectureEventFactory;
        this.structureModifier = StructureModifier.get(new FriendKey());
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
        final var threadPool0 = threadPool;
        threadPool0.shutdown();
        try
        {
            if (!threadPool0.awaitTermination(30, TimeUnit.SECONDS))
                log.atSevere().log(
                    "Timed out waiting to terminate DatabaseManager ExecutorService!" +
                        " The database may be out of sync with the world!"
                );
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                "Thread got interrupted waiting for DatabaseManager ExecutorService to terminate!" +
                    " The database may be out of sync with the world!",
                exception
            );
        }
    }

    private void initThreadPool()
    {
        this.threadPool = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Inserts a {@link Structure} into the database and assumes that the structure was NOT created by an
     * {@link IPlayer}. See {@link #addStructure(Structure, IPlayer)}.
     *
     * @param structure
     *     The new {@link Structure}.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public CompletableFuture<StructureInsertResult> addStructure(Structure structure)
    {
        return addStructure(structure, null);
    }

    /**
     * Inserts a {@link Structure} into the database.
     *
     * @param structure
     *     The new {@link Structure}.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareCreateEvent} and the {@link IStructureCreatedEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<StructureInsertResult> addStructure(
        Structure structure,
        @Nullable IPlayer responsible)
    {
        final var ret = callCancellableEvent(fact -> fact.createPrepareStructureCreateEvent(structure, responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return new StructureInsertResult(Optional.empty(), true);

                final Optional<Structure> result = db.insert(structure);
                result.ifPresentOrElse(
                    newStructure ->
                    {
                        powerBlockManager.get().invalidateChunkAt(
                            newStructure.getWorld().worldName(),
                            new Vector3Di(
                                newStructure.getPowerBlock().x(),
                                newStructure.getPowerBlock().y(),
                                newStructure.getPowerBlock().z())
                        );
                        newStructure.verifyRedstoneState();
                    },
                    () -> log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event)
                );

                return new StructureInsertResult(result, false);
            }, threadPool)
            .withExceptionContext(
                "Adding structure %s with responsible %s",
                structure.getBasicInfo(),
                responsible
            );

        ret.thenAccept(result -> result.structure.ifPresent(unused -> callStructureCreatedEvent(result, responsible)))
            .handleExceptional(ex ->
                log.atSevere().withCause(ex).log("Failed to call IStructureCreatedEvent for structure %s!", structure));

        return ret;
    }

    /**
     * Calls the {@link IStructureCreatedEvent}.
     *
     * @param result
     *     The result of trying to add a structure to the database.
     * @param responsible
     *     The {@link IPlayer} responsible for creating it, if an {@link IPlayer} was responsible for it. If not, this
     *     is null.
     */
    private void callStructureCreatedEvent(StructureInsertResult result, @Nullable IPlayer responsible)
    {
        CompletableFuture
            .runAsync(() ->
            {
                if (result.cancelled() || result.structure().isEmpty())
                    return;

                final IStructureCreatedEvent structureCreatedEvent =
                    animatedArchitectureEventFactory.createStructureCreatedEvent(result.structure().get(), responsible);

                animatedArchitectureEventCaller.callAnimatedArchitectureEvent(structureCreatedEvent);
            }, executor.getVirtualExecutor())
            .orTimeout(10, TimeUnit.SECONDS)
            .handleExceptional(ex ->
                log.atSevere().withCause(ex).log(
                    "Failed to call IStructureCreatedEvent for structure %s with player %s!",
                    result.structure(),
                    responsible
                ));
    }

    /**
     * Removes a {@link Structure} from the database and assumes that the structure was NOT deleted by an
     * {@link IPlayer}. See {@link #deleteStructure(Structure, IPlayer)}.
     *
     * @param structure
     *     The structure that will be deleted.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> deleteStructure(Structure structure)
    {
        return deleteStructure(structure, null);
    }

    /**
     * Removes a {@link Structure} from the database.
     *
     * @param structure
     *     The structure that will be deleted.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> deleteStructure(Structure structure, @Nullable IPlayer responsible)
    {
        return callCancellableEvent(fact -> fact.createPrepareDeleteStructureEvent(structure, responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return ActionResult.CANCELLED;

                final StructureSnapshot snapshot = structure.getSnapshot();
                final boolean result = db.removeStructure(snapshot.getUid());
                if (!result)
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event);
                    return ActionResult.FAIL;
                }

                structureDeletionManager.onStructureDeletion(snapshot);

                return ActionResult.SUCCESS;
            }, executor.getVirtualExecutor())
            .withExceptionContext(
                "Deleting structure %s with responsible %s",
                structure.getBasicInfo(),
                responsible
            );
    }

    /**
     * Gets a list of structure UIDs that have their rotation point in a given chunk.
     *
     * @param chunkX
     *     The x-coordinate of the chunk (in chunk space).
     * @param chunkZ
     *     The z-coordinate of the chunk (in chunk space).
     * @return A list of structure UIDs that have their rotation point in a given chunk.
     */
    public CompletableFuture<List<Structure>> getStructuresInChunk(int chunkX, int chunkZ)
    {
        final long chunkId = LocationUtil.getChunkId(chunkX, chunkZ);
        return CompletableFuture
            .supplyAsync(() -> db.getStructuresInChunk(chunkId), threadPool)
            .withExceptionContext("Retrieving structures in chunk %d", chunkId);
    }

    /**
     * Obtains all structures of a given type.
     *
     * @param typeName
     *     The name of the type. See {@link StructureType#getFullKey()}.
     * @return All structures of the given type.
     */
    public CompletableFuture<List<Structure>> getStructuresOfType(String typeName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructuresOfType(typeName), threadPool)
            .withExceptionContext("Retrieving structures of type '%s'", typeName);
    }

    /**
     * Obtains all structures of a specific version of a given type.
     *
     * @param typeName
     *     The name of the type. See {@link StructureType#getFullKey()}.
     * @param version
     *     The version of the type.
     * @return All structures of the given type and version.
     */
    public CompletableFuture<List<Structure>> getStructuresOfType(String typeName, int version)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructuresOfType(typeName, version), threadPool)
            .withExceptionContext("Retrieving structures of type '%s' with version %d", typeName, version);
    }

    /**
     * Gets all {@link Structure} owned by a player. Only searches for {@link Structure} with a given name if one was
     * provided.
     *
     * @param playerUUID
     *     The {@link UUID} of the payer.
     * @param structureID
     *     The name or the UID of the {@link Structure} to search for. Can be null.
     * @return All {@link Structure} owned by a player with a specific name.
     */
    public CompletableFuture<List<Structure>> getStructures(UUID playerUUID, String structureID)
    {
        // Check if the name is actually the UID of the structure.
        final OptionalLong structureUID = MathUtil.parseLong(structureID);
        if (structureUID.isPresent())
            return CompletableFuture
                .supplyAsync(() -> db.getStructure(playerUUID, structureUID.getAsLong())
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList()), threadPool)
                .withExceptionContext(
                    "Retrieving structure with UID %s for player with UUID %s",
                    structureUID.getAsLong(),
                    playerUUID
                );

        return CompletableFuture
            .supplyAsync(() -> db.getStructures(playerUUID, structureID), threadPool)
            .withExceptionContext(
                "Retrieving structures with name '%s' for player with UUID %s",
                structureID,
                playerUUID
            );
    }

    /**
     * See {@link #getStructures(UUID, String)}.
     */
    public CompletableFuture<List<Structure>> getStructures(IPlayer player, String name)
    {
        return getStructures(player.getUUID(), name);
    }

    /**
     * Gets all {@link Structure} owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return All {@link Structure} owned by a player.
     */
    public CompletableFuture<List<Structure>> getStructures(UUID playerUUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructures(playerUUID), threadPool)
            .withExceptionContext("Retrieving structures for player with UUID %s", playerUUID);
    }

    /**
     * See {@link #getStructures(UUID)}.
     */
    public CompletableFuture<List<Structure>> getStructures(IPlayer player)
    {
        return getStructures(player.getUUID());
    }

    /**
     * Gets all {@link Structure} owned by a player with a specific name.
     *
     * @param player
     *     The player whose structures to retrieve.
     * @param name
     *     The name of the {@link Structure} to search for.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the {@link Structure}s.
     * @return All {@link Structure} owned by a player with a specific name.
     */
    public CompletableFuture<List<Structure>> getStructures(
        IPlayer player,
        String name,
        PermissionLevel maxPermission)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructures(player.getUUID(), name, maxPermission), threadPool)
            .withExceptionContext(
                "Retrieving structures with name '%s' for player %s with max permission %s",
                name,
                player,
                maxPermission
            );
    }

    /**
     * Gets all {@link Structure}s with a specific name, regardless over ownership.
     *
     * @param name
     *     The name of the {@link Structure}s.
     * @return All {@link Structure}s with a specific name.
     */
    public CompletableFuture<List<Structure>> getStructures(String name)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructures(name), threadPool)
            .withExceptionContext("Retrieving structures with name '%s'", name);
    }

    /**
     * Updates the name of a player in the database, to make sure the player's name and UUID don't go out of sync.
     *
     * @param player
     *     The Player.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public CompletableFuture<Boolean> updatePlayer(IPlayer player)
    {
        return CompletableFuture
            .supplyAsync(() -> db.updatePlayerData(player.getPlayerData()), threadPool)
            .withExceptionContext("Updating player data for player %s", player);
    }

    /**
     * Tries to find the {@link PlayerData} for a player with the given {@link UUID}.
     *
     * @param uuid
     *     The {@link UUID} of a player.
     * @return The {@link PlayerData} that represents the player.
     */
    public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPlayerData(uuid), threadPool)
            .withExceptionContext("Retrieving player data for UUID %s", uuid);
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
    public CompletableFuture<List<PlayerData>> getPlayerData(String playerName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPlayerData(playerName), threadPool)
            .withExceptionContext("Retrieving player data for name '%s'", playerName);
    }

    /**
     * Gets the {@link Structure} with a specific UID.
     *
     * @param structureUID
     *     The UID of the {@link Structure}.
     * @return The {@link Structure} if it exists.
     */
    public CompletableFuture<Optional<Structure>> getStructure(long structureUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructure(structureUID), threadPool)
            .withExceptionContext("Retrieving structure with UID %d", structureUID);
    }

    /**
     * Gets the {@link Structure} with the given UID owned by the player. If the given player does not own the provided
     * structure, no structure will be returned.
     *
     * @param player
     *     The {@link IPlayer}.
     * @param structureUID
     *     The UID of the {@link Structure}.
     * @return The {@link Structure} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<Structure>> getStructure(IPlayer player, long structureUID)
    {
        return getStructure(player.getUUID(), structureUID)
            .withExceptionContext(
                "Retrieving structure with UID %d for player: %s",
                structureUID,
                player
            );
    }

    /**
     * Gets the {@link Structure} with the given UID owned by the player. If the given player does not own the *
     * provided structure, no structure will be returned.
     *
     * @param uuid
     *     The {@link UUID} of the player.
     * @param structureUID
     *     The UID of the {@link Structure}.
     * @return The {@link Structure} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<Structure>> getStructure(UUID uuid, long structureUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructure(uuid, structureUID), threadPool)
            .withExceptionContext(
                "Retrieving structure with UID %d for player with UUID %s",
                structureUID,
                uuid
            );
    }

    /**
     * Gets the number of {@link Structure}s owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return The number of {@link Structure}s this player owns.
     */
    public CompletableFuture<Integer> countStructuresOwnedByPlayer(UUID playerUUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructureCountForPlayer(playerUUID), threadPool)
            .withExceptionContext(
                "Retrieving structure count for player with UUID %s",
                playerUUID
            );
    }

    /**
     * Counts the number of {@link Structure}s with a specific name owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @param structureName
     *     The name of the structure.
     * @return The number of {@link Structure}s with a specific name owned by a player.
     */
    public CompletableFuture<Integer> countStructuresOwnedByPlayer(UUID playerUUID, String structureName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructureCountForPlayer(playerUUID, structureName), threadPool)
            .withExceptionContext(
                "Retrieving structure for player with UUID %s and structure name '%s'",
                playerUUID,
                structureName
            );
    }

    /**
     * The number of {@link Structure}s in the database with a specific name.
     *
     * @param structureName
     *     The name of the {@link Structure}.
     * @return The number of {@link Structure}s with a specific name.
     */
    public CompletableFuture<Integer> countStructuresByName(String structureName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructureCountByName(structureName), threadPool)
            .withExceptionContext(
                "Retrieving structure count for structure name '%s'",
                structureName
            );
    }

    /**
     * Adds a player as owner to a {@link Structure} at a given level of ownership and assumes that the structure was
     * NOT deleted by an {@link IPlayer}. See {@link #addOwner(Structure, IPlayer, PermissionLevel, IPlayer)}.
     *
     * @param structure
     *     The {@link Structure}.
     * @param player
     *     The {@link IPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(
        Structure structure,
        IPlayer player,
        PermissionLevel permission)
    {
        return addOwner(structure, player, permission, null);
    }

    /**
     * Adds a player as owner to a {@link Structure} at a given level of ownership.
     *
     * @param structure
     *     The {@link Structure}.
     * @param player
     *     The {@link IPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(
        Structure structure,
        IPlayer player,
        PermissionLevel permission,
        @Nullable IPlayer responsible)
    {
        if (permission.getValue() < 1 || permission == PermissionLevel.NO_PERMISSION)
            return CompletableFuture.completedFuture(ActionResult.FAIL);

        final var newOwner = new StructureOwner(structure.getUid(), permission, player.getPlayerData());

        return callCancellableEvent(fact -> fact.createStructurePrepareAddOwnerEvent(structure, newOwner, responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return ActionResult.CANCELLED;

                if (!structureModifier.addOwner(structure, newOwner))
                {
                    log.atSevere().log("Failed to add owner %s to structure %s!", newOwner, structure);
                    return ActionResult.FAIL;
                }

                final boolean result = db.addOwner(structure.getUid(), newOwner.playerData(), permission);
                if (!result)
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event);
                    structureModifier.removeOwner(structure, newOwner.playerData().getUUID());
                    return ActionResult.FAIL;
                }

                return ActionResult.SUCCESS;
            }, threadPool)
            .withExceptionContext(
                "Adding owner %s to structure %s with permission %s with responsible %s",
                player,
                structure.getBasicInfo(),
                permission,
                responsible
            );
    }

    /**
     * Calls an {@link ICancellableAnimatedArchitectureEvent} and checks if it was cancelled or not.
     *
     * @param factoryMethod
     *     The method to use to construct the event.
     * @return True if the create event was cancelled, otherwise false.
     */
    private <T extends ICancellableAnimatedArchitectureEvent> CompletableFuture<T> callCancellableEvent(
        Function<IAnimatedArchitectureEventFactory, T> factoryMethod)
    {
        final T event = factoryMethod.apply(animatedArchitectureEventFactory);
        return CompletableFuture.supplyAsync(() ->
            {
                log.atFinest().log("Calling event: %s", event);
                animatedArchitectureEventCaller.callAnimatedArchitectureEvent(event);
                log.atFinest().log("Processed event: %s", event);
                return event;
            }, executor.getVirtualExecutor())
            .withExceptionContext("Calling event %s", event);
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link Structure}.
     *
     * @param structure
     *     The {@link Structure}.
     * @param player
     *     The {@link IPlayer}.
     * @return True if owner removal was successful.
     */
    public CompletableFuture<ActionResult> removeOwner(Structure structure, IPlayer player)
    {
        return removeOwner(structure, player, null);
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link Structure}.
     *
     * @param structure
     *     The {@link Structure}.
     * @param player
     *     The {@link IPlayer}.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        Structure structure,
        IPlayer player,
        @Nullable IPlayer responsible)
    {
        return removeOwner(structure, player.getUUID(), responsible);
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link Structure} and assumes that the structure was NOT deleted by an
     * {@link IPlayer}. See {@link #removeOwner(Structure, UUID, IPlayer)}.
     *
     * @param structure
     *     The {@link Structure}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPlayer}.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(Structure structure, UUID playerUUID)
    {
        return removeOwner(structure, playerUUID, null);
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link Structure}.
     *
     * @param structure
     *     The {@link Structure}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPlayer}.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        Structure structure,
        UUID playerUUID,
        @Nullable IPlayer responsible)
    {
        final Optional<StructureOwner> structureOwner = structure.getOwner(playerUUID);
        if (structureOwner.isEmpty())
        {
            log.atFine().log(
                "Trying to remove player: %s from structure: %d, but the player is not an owner!",
                playerUUID,
                structure.getUid()
            );
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }
        if (structureOwner.get().permission() == PermissionLevel.CREATOR)
        {
            log.atFine().log(
                "Trying to remove player: %s from structure: %d, but the player is the prime owner! " +
                    "This is not allowed!",
                playerUUID,
                structure.getUid()
            );
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }

        return callCancellableEvent(
            fact -> fact.createStructurePrepareRemoveOwnerEvent(structure, structureOwner.get(), responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return ActionResult.CANCELLED;

                final @Nullable StructureOwner oldOwner = structureModifier.removeOwner(structure, playerUUID);
                if (oldOwner == null)
                {
                    log.atSevere().log("Failed to remove owner %s from structure %s!", structureOwner.get(), structure);
                    return ActionResult.FAIL;
                }

                final boolean result = db.removeOwner(structure.getUid(), playerUUID);
                if (!result)
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event);
                    structureModifier.addOwner(structure, oldOwner);
                    return ActionResult.FAIL;
                }

                return ActionResult.SUCCESS;
            }, threadPool)
            .withExceptionContext(
                "Removing owner %s (%s) from structure %s with responsible %s",
                playerUUID,
                structureOwner,
                structure.getBasicInfo(),
                responsible
            );
    }

    /**
     * Updates the all data of an {@link Structure}. This includes both the base data and the type-specific data.
     *
     * @param snapshot
     *     The {@link Structure} that describes the base data of structure.
     * @return The result of the operation.
     */
    public CompletableFuture<DatabaseManager.ActionResult> syncStructureData(StructureSnapshot snapshot)
    {
        return CompletableFuture
            .supplyAsync(() -> db.syncStructureData(snapshot) ? ActionResult.SUCCESS : ActionResult.FAIL, threadPool)
            .withExceptionContext("Syncing structure data for structure %s", snapshot);
    }

    /**
     * Retrieves all {@link StructureIdentifier}s that start with the provided input.
     * <p>
     * For example, this method can retrieve the identifiers "1", "10", "11", "100", etc from an input of "1" or
     * "MyDoor", "MyPortcullis", "MyOtherStructure", etc. from an input of "My".
     *
     * @param input
     *     The partial identifier to look for.
     * @param player
     *     The player that should own the structures. May be null to disregard ownership.
     * @param properties
     *     The properties that the structures must have. When specified, only structures that have all of these
     *     properties will be returned.
     * @return All {@link StructureIdentifier}s that start with the provided input.
     */
    public CompletableFuture<List<StructureIdentifier>> getIdentifiersFromPartial(
        String input,
        @Nullable IPlayer player,
        PermissionLevel maxPermission,
        Collection<Property<?>> properties
    )
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPartialIdentifiers(input, player, maxPermission, properties), threadPool)
            .withExceptionContext(
                "Retrieving identifiers from partial input '%s' for player %s and max permission %s and properties: %s",
                input,
                player,
                maxPermission,
                properties
            );
    }

    /**
     * Checks if a world contains any structures.
     *
     * @param worldName
     *     The name of the world.
     * @return True if at least 1 structure exists in the world.
     */
    CompletableFuture<Boolean> isAnimatedArchitectureWorld(String worldName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.isAnimatedArchitectureWorld(worldName), threadPool)
            .withExceptionContext("Checking if world %s is an AnimatedArchitecture world", worldName);
    }

    /**
     * Gets a map of location hashes and their connected powerblocks for all structures in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the structures whose powerblocks
     * occupies that location.
     *
     * @param chunkId
     *     The id of the chunk the structures are in.
     * @return A map of location hashes and their connected powerblocks for all structures in a chunk.
     */
    CompletableFuture<Int2ObjectMap<LongList>> getPowerBlockData(long chunkId)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPowerBlockData(chunkId), threadPool)
            .withExceptionContext(
                "Retrieving power block data for chunk %d",
                chunkId
            );
    }

    @Override
    public String getDebugInformation()
    {
        return "Database " + StringUtil.toString(threadPool);
    }

    /**
     * Represents the result of an action requested from the database. E.g. deleting a structure.
     */
    public enum ActionResult
    {
        /**
         * The request was cancelled. E.g. by an {@link ICancellableAnimatedArchitectureEvent} event.
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

    public static final class FriendKey
    {
        private FriendKey()
        {
        }
    }

    /**
     * Contains the result of an attempt to insert a structure into the database.
     *
     * @param structure
     *     The structure as it was inserted into the database. This will be empty when inserted failed.
     * @param cancelled
     *     Whether the insertion was cancelled. An insertion may be cancelled if some listener cancels the
     *     {@link IStructurePrepareCreateEvent} event.
     */
    public record StructureInsertResult(Optional<Structure> structure, boolean cancelled)
    {
    }

    /**
     * Represents the identifier of a structure. This is a combination of the UID and the name of the structure.
     *
     * @param type
     *     The type of the structure.
     * @param uid
     *     The UID of the structure.
     * @param name
     *     The name of the structure.
     */
    public record StructureIdentifier(StructureType type, long uid, String name)
    {
    }
}
